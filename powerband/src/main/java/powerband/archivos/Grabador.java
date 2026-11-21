package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.LongSupplier;

import powerband.comandos.Comando;
import powerband.comandos.ObservadorComandos;
import powerband.excepciones.ArchivoException;
import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.EstadoInvalidoException;
import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

public final class Grabador implements ObservadorComandos {

    private static final long NANOS_POR_MILISEGUNDO = 1_000_000L;

    private final Path directorioGrabaciones;
    private final LongSupplier relojMonotonico;

    private final Object bloqueo = new Object();
    private boolean grabando;
    private long tiempoBaseNanos;
    private Path archivoSesionTemporal;
    private ArchivoRegistros<ComandoGrabado> archivoTemporal;

    public Grabador(Path directorioGrabaciones) {
        this(directorioGrabaciones, System::nanoTime);
    }

    public Grabador(Path directorioGrabaciones,
            LongSupplier relojMonotonico) {
        this.directorioGrabaciones = Objects.requireNonNull(
                directorioGrabaciones, "directorioGrabaciones")
                .toAbsolutePath().normalize();
        this.relojMonotonico = Objects.requireNonNull(
                relojMonotonico, "relojMonotonico");
    }

    public void iniciar() throws IOException {
        synchronized (bloqueo) {
            if (grabando) {
                throw EstadoInvalidoException.grabacionActiva();
            }

            Path temporal = null;
            try {
                Files.createDirectories(directorioGrabaciones);
                temporal = Files.createTempFile(
                        directorioGrabaciones,
                        "grabacion-", ".tmp");
                ArchivoRegistros<ComandoGrabado> archivo =
                        crearArchivoTemporal(temporal);
                if (archivo.cantidadRegistros() != 0) {
                    archivo.close();
                    throw ArchivoException.errorES(
                            temporal.toString(),
                            "iniciar una sesión temporal", null);
                }

                tiempoBaseNanos = relojMonotonico.getAsLong();
                archivoTemporal = archivo;
                archivoSesionTemporal = temporal;
                grabando = true;
            } catch (IOException excepcion) {
                if (temporal != null) {
                    eliminarTemporal(temporal, excepcion);
                }
                ArchivoException fallo = ArchivoException.errorES(
                        directorioGrabaciones.toString(),
                        "iniciar la grabación", excepcion);
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        directorioGrabaciones.toString());
                throw fallo;
            } catch (RuntimeException excepcion) {
                if (temporal != null) {
                    eliminarTemporal(temporal, excepcion);
                }
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        excepcion.getMessage(),
                        directorioGrabaciones.toString());
                throw excepcion;
            }
        }
    }

    public void detener() throws IOException {
        synchronized (bloqueo) {
            if (!grabando) {
                throw EstadoInvalidoException.grabacionInactiva();
            }

            grabando = false;
            try {
                archivoTemporal.close();
            } catch (IOException excepcion) {
                ArchivoException fallo = ArchivoException.errorES(
                        archivoSesionTemporal.toString(),
                        "cerrar la sesión de grabación", excepcion);
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        archivoSesionTemporal.toString());
                throw fallo;
            }
        }
    }

    public boolean estaGrabando() {
        synchronized (bloqueo) {
            return grabando;
        }
    }

    public Path archivoSesionTemporal() {
        synchronized (bloqueo) {
            if (archivoSesionTemporal == null) {
                throw EstadoInvalidoException.grabacionInactiva();
            }
            return archivoSesionTemporal;
        }
    }

    public long cantidadComandos() throws IOException {
        synchronized (bloqueo) {
            if (archivoSesionTemporal == null) {
                throw EstadoInvalidoException.grabacionInactiva();
            }
            try (ArchivoRegistros<ComandoGrabado> archivo =
                    crearArchivoTemporal(archivoSesionTemporal)) {
                return archivo.cantidadRegistros();
            }
        }
    }

    //En este caso evita errores con excepciones para facilitar el manejo
    @Override
    public boolean comandoEnviado(Comando comando, long momentoNanos) {
        synchronized (bloqueo) {
            if (!grabando) {
                return false;
            }
            if (comando == null) {
                ArgumentoInvalidoException fallo =
                        ArgumentoInvalidoException.comandoNulo();
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        archivoSesionTemporal.toString());
                return false;
            }

            long diferenciaNanos = momentoNanos - tiempoBaseNanos;
            if (diferenciaNanos < 0) {
                ArgumentoInvalidoException fallo =
                        ArgumentoInvalidoException.momentoAnterior();
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        archivoSesionTemporal.toString());
                return false;
            }

            try {
                long posicion = cantidadRegistrosSinValidar()
                        * Constantes.TAMANO_COMANDO_GRABADO;
                ComandoGrabado registro = new ComandoGrabado(
                        diferenciaNanos / NANOS_POR_MILISEGUNDO,
                        comando.destino().id(),
                        comando.tipoAccion(),
                        comando.parametros());
                archivoTemporal.escribir(posicion, registro);
                return true;
            } catch (IOException excepcion) {
                ArchivoException fallo = ArchivoException.errorES(
                        archivoSesionTemporal.toString(),
                        "guardar un comando grabado", excepcion);
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        archivoSesionTemporal.toString());
                return false;
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        excepcion.getMessage(),
                        archivoSesionTemporal.toString());
                return false;
            }
        }
    }

    private long cantidadRegistrosSinValidar() throws IOException {
        return archivoTemporal.cantidadRegistros();
    }

    private ArchivoRegistros<ComandoGrabado> crearArchivoTemporal(
            Path temporal) {
        return new ArchivoRegistros<>(
                temporal,
                Constantes.TAMANO_COMANDO_GRABADO,
                ComandoGrabado::desdeBytes,
                ComandoGrabado::serializar);
    }

    private void eliminarTemporal(Path temporal, Throwable fallo) {
        try {
            Files.deleteIfExists(temporal);
        } catch (IOException excepcionLimpieza) {
            fallo.addSuppressed(excepcionLimpieza);
        }
    }
}
