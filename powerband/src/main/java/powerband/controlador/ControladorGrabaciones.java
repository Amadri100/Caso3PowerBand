package powerband.controlador;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ValidacionesGenerales;
import powerband.comandos.BusComandos;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.FinalizadorGrabacion;
import powerband.grabaciones.GrabacionAbierta;
import powerband.grabaciones.Grabador;
import powerband.grabaciones.LimpiezaGrabacion;
import powerband.logging.LoggerPowerBand;
import powerband.logging.OrigenLog;
import powerband.grabaciones.Reproductor;

final class ControladorGrabaciones {

    private final BusComandos busComandos;
    private final Grabador grabador;
    private final CargadorGrabaciones cargador;
    private final FinalizadorGrabacion finalizador;
    private final Reproductor reproductor;
    private final ArchivosGrabacion archivos;
    private volatile Thread hiloReproduccion;

    protected ControladorGrabaciones(
            BusComandos busComandos, Grabador grabador,
            CargadorGrabaciones cargador, FinalizadorGrabacion finalizador,
            Reproductor reproductor, ArchivosGrabacion archivos) {
        this.busComandos = Objects.requireNonNull(busComandos, "busComandos");
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.cargador = Objects.requireNonNull(cargador, "cargador");
        this.finalizador = Objects.requireNonNull(finalizador, "finalizador");
        this.reproductor = Objects.requireNonNull(reproductor, "reproductor");
        this.archivos = Objects.requireNonNull(archivos, "archivos");
    }

    void iniciar() throws IOException {
        grabador.iniciar();
        busComandos.registrarObservador(grabador);
    }

    void finalizar(String nombre) throws IOException {
        busComandos.quitarObservador(grabador);
        finalizador.finalizar(nombre);
    }

    protected List<String> listarSesiones() throws IOException {
        List<String> nombres = new ArrayList<>();
        for (EntradaListaGrabacion entrada : cargador.listar()) {
            nombres.add(entrada.nombre());
        }
        return List.copyOf(nombres);
    }

    protected List<String> buscarSesiones(String nombre) throws IOException {
        ValidacionesGenerales.validarNombre(nombre);
        String filtro = nombre.toLowerCase(Locale.ROOT);
        List<String> coincidencias = new ArrayList<>();
        for (String sesion : listarSesiones()) {
            if (sesion.toLowerCase(Locale.ROOT).contains(filtro)) {
                coincidencias.add(sesion);
            }
        }
        return List.copyOf(coincidencias);
    }

    protected Optional<GrabacionAbierta> abrir(String nombre) throws IOException {
        return cargador.abrir(nombre);
    }

    void iniciarReproduccion(
            String nombre, GrabacionAbierta grabacion, Consumer<Throwable> alFinalizar) {
        Thread hilo = new Thread(() -> {
            Throwable fallo = null;
            try {
                reproductor.reproducir(grabacion);
            } catch (IOException | InterruptedException | RuntimeException | Error excepcion) {
                if (excepcion instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                fallo = excepcion;
            }
            try {
                alFinalizar.accept(fallo);
            } finally {
                synchronized (this) {
                    if (hiloReproduccion == Thread.currentThread()) {
                        hiloReproduccion = null;
                    }
                }
            }
        }, "reproduccion-" + nombre);
        hilo.setDaemon(true);
        synchronized (this) {
            hiloReproduccion = hilo;
        }
        try {
            hilo.start();
        } catch (RuntimeException | Error excepcion) {
            synchronized (this) {
                if (hiloReproduccion == hilo) {
                    hiloReproduccion = null;
                }
            }
            throw excepcion;
        }
    }

    boolean estaGrabando() {
        return grabador.estaGrabando();
    }

    boolean hayReproduccion() {
        return hiloReproduccion != null;
    }

    synchronized void finalizarReproduccion() {
        if (hiloReproduccion == Thread.currentThread()) {
            hiloReproduccion = null;
        }
    }

    protected Throwable abortarGrabacion(Throwable fallo, boolean grabando) {
        if (!grabando && !grabador.estaGrabando()) {
            return fallo;
        }
        busComandos.quitarObservador(grabador);
        Path archivoTemporal = null;
        try {
            archivoTemporal = grabador.archivoSesionTemporal();
        } catch (RuntimeException excepcion) {
            fallo = FallosControlador.agregar(
                    fallo, "No se pudo localizar el temporal de grabación", excepcion);
        }
        try {
            if (grabador.estaGrabando()) {
                grabador.detener();
            }
        } catch (IOException | RuntimeException excepcion) {
            fallo = FallosControlador.agregar(
                    fallo, "No se pudo detener la grabación al desconectar", excepcion);
        }
        if (archivoTemporal != null) {
            try {
                new LimpiezaGrabacion(archivoTemporal, archivos).limpiarExitosamente();
            } catch (IOException | RuntimeException excepcion) {
                fallo = FallosControlador.agregar(
                        fallo, "No se pudo limpiar el temporal de grabación", excepcion);
            }
        }
        return fallo;
    }

    protected Throwable detenerReproduccion(Throwable fallo) {
        try {
            reproductor.detener();
        } catch (RuntimeException excepcion) {
            fallo = FallosControlador.agregar(
                    fallo, "No se pudo detener la reproducción", excepcion);
        }
        boolean interrumpido = false;
        Thread hilo = hiloReproduccion;
        if (hilo != null && hilo != Thread.currentThread()) {
            if (hilo.isAlive()) {
                hilo.interrupt();
            }
            while (hilo.isAlive()) {
                try {
                    hilo.join();
                } catch (InterruptedException excepcion) {
                    LoggerPowerBand.getInstance().registrarExcepcionNoCapturada(
                            excepcion, OrigenLog.CONTROLADOR);
                    interrumpido = true;
                }
            }
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
            fallo = FallosControlador.agregar(
                    fallo, "Se interrumpió la espera de finalización de la reproducción",
                    new InterruptedException("La espera de reproducción fue interrumpida"));
        }
        return fallo;
    }
}
