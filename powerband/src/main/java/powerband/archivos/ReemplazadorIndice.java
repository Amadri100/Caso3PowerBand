package powerband.archivos;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

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
        try {
            validarArchivos();
            validarResultado();
            moverResultado();
        } catch (IOException excepcion) {
            IOException fallo = informarFallo(excepcion);
            LoggerPowerBand.getInstance().registrarErrorArchivo(
                    fallo.getMessage(), rutaIndice.toString());
            throw fallo;
        } catch (RuntimeException excepcion) {
            LoggerPowerBand.getInstance().registrarErrorArchivo(
                    "No se pudo reemplazar el índice " + rutaIndice + ": "
                            + excepcion.getMessage(),
                    rutaIndice.toString());
            throw excepcion;
        }
    }
    //Validaciones de archivo, directorio etc
    private void validarArchivos() throws IOException {
        if (!Files.isRegularFile(rutaIndice)) {
            throw new IOException(
                    "La ruta del índice no es un archivo regular");
        }
        if (!Files.isRegularFile(rutaResultado)) {
            throw new IOException(
                    "El resultado ordenado no es un archivo regular");
        }

        Path directorioIndice = rutaIndice.getParent();
        Path directorioResultado = rutaResultado.getParent();
        if (directorioIndice == null
                || !directorioIndice.equals(directorioResultado)) {
            throw new IOException(
                    "El índice y su resultado deben estar en el mismo "
                            + "directorio");
        }
        if (rutaIndice.equals(rutaResultado)) {
            throw new IOException(
                    "El resultado no puede reemplazar directamente su origen");
        }
    }
    //Validacion de duplicados y ordenados.
    private void validarResultado() throws IOException {
        long cantidadIndice;
        long cantidadResultado;
        try (ArchivoRegistros<EntradaIndice> indice =
                    crearArchivoRegistros(rutaIndice);
                ArchivoRegistros<EntradaIndice> resultado =
                    crearArchivoRegistros(rutaResultado)) {
            cantidadIndice = indice.cantidadRegistros();
            cantidadResultado = resultado.cantidadRegistros();
        }

        if (cantidadResultado != cantidadIndice) {
            throw new IOException(
                    "El resultado tiene " + cantidadResultado
                            + " registros; se esperaban " + cantidadIndice);
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
                    throw new IOException(
                            "El resultado no está estrictamente ordenado "
                                    + "o contiene nombres duplicados");
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
            throw new IOException(
                    "El sistema de archivos no permite reemplazar "
                            + "recordings.index de forma atomica",
                    excepcion);
        }
    }
    //Crea la excepcion
    private IOException informarFallo(IOException excepcion) {
        String mensaje = "No se pudo reemplazar el índice " + rutaIndice
                + " con " + rutaResultado + ": " + excepcion.getMessage();
        return new IOException(mensaje, excepcion);
    }
    //Obtener un archivo del indice
    private static ArchivoRegistros<EntradaIndice> crearArchivoRegistros(
            Path ruta) {
        return new ArchivoRegistros<>(ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
