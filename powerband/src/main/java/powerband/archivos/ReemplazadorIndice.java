package powerband.archivos;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.util.Constantes;
import powerband.excepciones.ArchivoException;
import powerband.excepciones.CantidadInesperadaException;
import powerband.excepciones.OrdenamientoException;
import powerband.excepciones.tipos.TipoErrorArchivo;
import powerband.excepciones.tipos.TipoErrorOrdenamiento;

public final class ReemplazadorIndice {

    private final Path rutaIndice;
    private final Path rutaResultado;

    public ReemplazadorIndice(Path rutaIndice, Path rutaResultado) {
        this.rutaIndice = Objects.requireNonNull(
                rutaIndice, "rutaIndice").toAbsolutePath().normalize();
        this.rutaResultado = Objects.requireNonNull(
                rutaResultado, "rutaResultado").toAbsolutePath().normalize();
    }
    //Realiza validaciones y lo muevo, 
    //En caso de error manda una excepcion
    public void reemplazar() throws IOException {
        try (ArchivoRegistros<EntradaIndice> indice =
                crearArchivoRegistros(rutaIndice)) {
            reemplazar(indice.cantidadRegistros());
        }
    }

    public void reemplazar(long cantidadResultadoEsperada)
            throws IOException {
        try {
            validarArchivos();
            validarResultado(cantidadResultadoEsperada);
            moverResultado();
        } catch (ArchivoException excepcion) {
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    excepcion.getMessage(), rutaIndice.toString(), OrigenLog.ARCHIVOS, excepcion));
            throw excepcion;
        } catch (IOException excepcion) {
            ArchivoException fallo = ArchivoException.errorES(
                    rutaIndice.toString(), "reemplazar el índice", excepcion);
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    fallo.getMessage(), rutaIndice.toString(), OrigenLog.ARCHIVOS, fallo));
            throw fallo;
        } catch (RuntimeException excepcion) {
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    "No se pudo reemplazar el índice " + rutaIndice + ": "
                            + excepcion.getMessage(),
                    rutaIndice.toString(), OrigenLog.ARCHIVOS, excepcion));
            throw excepcion;
        }
    }
    //Validaciones de archivo, directorio etc
    private void validarArchivos() throws IOException {
        if (!Files.isRegularFile(rutaIndice)) {
            throw ArchivoException.rutaNoRegular(rutaIndice.toString());
        }
        if (!Files.isRegularFile(rutaResultado)) {
            throw ArchivoException.rutaNoRegular(rutaResultado.toString());
        }

        Path directorioIndice = rutaIndice.getParent();
        Path directorioResultado = rutaResultado.getParent();
        if (directorioIndice == null
                || !directorioIndice.equals(directorioResultado)) {
            throw ArchivoException.rutaNoRegular(rutaResultado.toString());
        }
        if (rutaIndice.equals(rutaResultado)) {
            throw ArchivoException.rutaNoRegular(rutaResultado.toString());
        }
    }
    //Validacion de duplicados y ordenados.
    private void validarResultado(long cantidadResultadoEsperada)
            throws IOException {
        long cantidadResultado;
        try (ArchivoRegistros<EntradaIndice> indice =
                    crearArchivoRegistros(rutaIndice);
                ArchivoRegistros<EntradaIndice> resultado =
                    crearArchivoRegistros(rutaResultado)) {
            cantidadResultado = resultado.cantidadRegistros();
        }

        if (cantidadResultado != cantidadResultadoEsperada) {
            throw new CantidadInesperadaException(
                    rutaResultado.toString(), cantidadResultadoEsperada,
                    cantidadResultado);
        }

        EntradaIndice anterior = null;
        try (ArchivoRegistros<EntradaIndice> resultado =
                crearArchivoRegistros(rutaResultado)) {
            for (long indice = 0; indice < cantidadResultado; indice++) {
                EntradaIndice actual = resultado.leer(
                        indice * Constantes.TAMANO_ENTRADA_INDICE);
                if (anterior != null
                        && ComparadorNombres.INSTANCIA.compare(
                                anterior.nombre(), actual.nombre()) >= 0) {
                    throw new OrdenamientoException(
                            TipoErrorOrdenamiento.INDICE_DESORDENADO,
                            rutaResultado.toString(), actual.nombre());
                }
                anterior = actual;
            }
        }
    }
    //Mueve del resultado al indice, 
    //Hace un remplazo completo
    //se ejecuta de forma atomica, en escencia eso significa 
    //que el archivo no queda en ningun estado corrompido si sucede algun error
    private void moverResultado() throws IOException {
        try {
            Files.move(rutaResultado, rutaIndice,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException excepcion) {
            throw new ArchivoException(
                    TipoErrorArchivo.REEMPLAZO_NO_ATOMICO,
                    rutaIndice.toString(),
                    excepcion);
        } catch (java.nio.file.AccessDeniedException excepcion) {
            try {
                Files.move(rutaResultado, rutaIndice,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallback) {
                excepcion.addSuppressed(fallback);
                throw new ArchivoException(
                        TipoErrorArchivo.REEMPLAZO_NO_ATOMICO,
                        rutaIndice.toString(),
                        excepcion);
            }
        } catch (IOException excepcion) {
            throw new ArchivoException(
                    TipoErrorArchivo.REEMPLAZO_NO_ATOMICO,
                    rutaIndice.toString(),
                    excepcion);
        }
    }
    //Crea la excepcion
    //Obtener un archivo del indice
    private static ArchivoRegistros<EntradaIndice> crearArchivoRegistros(
            Path ruta) {
        return new ArchivoRegistros<>(ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
