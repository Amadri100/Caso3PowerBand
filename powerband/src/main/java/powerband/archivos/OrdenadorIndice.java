package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

public final class OrdenadorIndice {

    private final Path rutaIndice;
    private final ArchivoRegistros<EntradaIndice> archivoIndice;

    public OrdenadorIndice(Path rutaIndice) {
        this.rutaIndice = Objects.requireNonNull(rutaIndice, "rutaIndice")
                .toAbsolutePath().normalize();
        this.archivoIndice = crearArchivoRegistros(this.rutaIndice);
    }

    public Path ordenar() throws IOException {
        Set<Path> temporales = new LinkedHashSet<>();
        try {
            long cantidad = archivoIndice.cantidadRegistros();
            Path origenOrdenado = rutaIndice;
            long longitudRun = 1;

            if (cantidad <= 1) {
                Path salida = crearTemporal(temporales);
                copiar(origenOrdenado, salida, cantidad);
                validarResultado(salida, cantidad);
                return entregarResultado(salida, temporales);
            }

            while (longitudRun < cantidad) {
                Path entradaIzquierda = crearTemporal(temporales);
                Path entradaDerecha = crearTemporal(temporales);
                Path salida = crearTemporal(temporales);

                distribuirRuns(origenOrdenado, entradaIzquierda,
                        entradaDerecha, cantidad, longitudRun);
                mezclarRuns(entradaIzquierda, entradaDerecha, salida,
                        longitudRun);
                validarCantidad(salida, cantidad);

                eliminarTemporal(entradaIzquierda, temporales);
                eliminarTemporal(entradaDerecha, temporales);
                if (!origenOrdenado.equals(rutaIndice)) {
                    eliminarTemporal(origenOrdenado, temporales);
                }

                origenOrdenado = salida;
                longitudRun = longitudRun > cantidad / 2
                        ? cantidad : longitudRun * 2;
            }

            validarResultado(origenOrdenado, cantidad);
            return entregarResultado(origenOrdenado, temporales);
        } catch (IOException excepcion) {
            IOException fallo = new IOException(
                    "No se pudo ordenar el índice " + rutaIndice + ": "
                            + excepcion.getMessage(),
                    excepcion);
            limpiarTemporales(temporales, fallo);
            LoggerPowerBand.getInstance().registrarErrorArchivo(
                    fallo.getMessage(), rutaIndice.toString());
            throw fallo;
        } catch (RuntimeException excepcion) {
            limpiarTemporales(temporales, excepcion);
            throw excepcion;
        }
    }

    private void distribuirRuns(Path origen, Path rutaIzquierda,
            Path rutaDerecha, long cantidad, long longitudRun)
            throws IOException {
        ArchivoRegistros<EntradaIndice> archivoOrigen =
                crearArchivoRegistros(origen);
        ArchivoRegistros<EntradaIndice> izquierda =
                crearArchivoRegistros(rutaIzquierda);
        ArchivoRegistros<EntradaIndice> derecha =
                crearArchivoRegistros(rutaDerecha);

        long indiceOrigen = 0;
        long cantidadIzquierda = 0;
        long cantidadDerecha = 0;
        long numeroRun = 0;
        while (indiceOrigen < cantidad) {
            long longitudActual = Math.min(
                    longitudRun, cantidad - indiceOrigen);
            boolean escribirIzquierda = numeroRun % 2 == 0;
            for (long indiceRun = 0;
                    indiceRun < longitudActual; indiceRun++) {
                EntradaIndice entrada =
                        archivoOrigen.leer(posicionBytes(indiceOrigen));
                if (escribirIzquierda) {
                    izquierda.escribir(
                            posicionBytes(cantidadIzquierda), entrada);
                    cantidadIzquierda++;
                } else {
                    derecha.escribir(
                            posicionBytes(cantidadDerecha), entrada);
                    cantidadDerecha++;
                }
                indiceOrigen++;
            }
            numeroRun++;
        }
    }

    private void mezclarRuns(Path rutaIzquierda, Path rutaDerecha,
            Path rutaSalida, long longitudRun) throws IOException {
        ArchivoRegistros<EntradaIndice> izquierda =
                crearArchivoRegistros(rutaIzquierda);
        ArchivoRegistros<EntradaIndice> derecha =
                crearArchivoRegistros(rutaDerecha);
        ArchivoRegistros<EntradaIndice> salida =
                crearArchivoRegistros(rutaSalida);
        long cantidadIzquierda = izquierda.cantidadRegistros();
        long cantidadDerecha = derecha.cantidadRegistros();
        long indiceIzquierda = 0;
        long indiceDerecha = 0;
        long indiceSalida = 0;

        while (indiceIzquierda < cantidadIzquierda
                || indiceDerecha < cantidadDerecha) {
            long finRunIzquierdo = Math.min(
                    indiceIzquierda + longitudRun, cantidadIzquierda);
            long finRunDerecho = Math.min(
                    indiceDerecha + longitudRun, cantidadDerecha);
            while (indiceIzquierda < finRunIzquierdo
                    || indiceDerecha < finRunDerecho) {
                EntradaIndice entradaIzquierda =
                        indiceIzquierda < finRunIzquierdo
                                ? izquierda.leer(
                                        posicionBytes(indiceIzquierda))
                                : null;
                EntradaIndice entradaDerecha =
                        indiceDerecha < finRunDerecho
                                ? derecha.leer(posicionBytes(indiceDerecha))
                                : null;

                if (entradaDerecha == null
                        || entradaIzquierda != null
                                && ComparadorNombres.INSTANCIA.compare(
                                        entradaIzquierda.nombre(),
                                        entradaDerecha.nombre()) <= 0) {
                    salida.escribir(posicionBytes(indiceSalida++),
                            entradaIzquierda);
                    indiceIzquierda++;
                } else {
                    salida.escribir(posicionBytes(indiceSalida++),
                            entradaDerecha);
                    indiceDerecha++;
                }
            }
        }
    }

    private void copiar(Path rutaOrigen, Path rutaDestino, long cantidad)
            throws IOException {
        ArchivoRegistros<EntradaIndice> origen =
                crearArchivoRegistros(rutaOrigen);
        ArchivoRegistros<EntradaIndice> destino =
                crearArchivoRegistros(rutaDestino);
        for (long indice = 0; indice < cantidad; indice++) {
            destino.escribir(posicionBytes(indice),
                    origen.leer(posicionBytes(indice)));
        }
    }

    private void validarResultado(Path rutaResultado, long cantidadEsperada)
            throws IOException {
        ArchivoRegistros<EntradaIndice> resultado =
                crearArchivoRegistros(rutaResultado);
        validarCantidad(rutaResultado, cantidadEsperada);
        EntradaIndice anterior = null;
        for (long indice = 0; indice < cantidadEsperada; indice++) {
            EntradaIndice actual = resultado.leer(posicionBytes(indice));
            if (anterior != null && ComparadorNombres.INSTANCIA.compare(
                    anterior.nombre(), actual.nombre()) >= 0) {
                throw new IOException(
                        "El índice contiene nombres duplicados o no "
                                + "quedó estrictamente ordenado");
            }
            anterior = actual;
        }
    }

    private void validarCantidad(Path ruta, long cantidadEsperada)
            throws IOException {
        long cantidadObtenida =
                crearArchivoRegistros(ruta).cantidadRegistros();
        if (cantidadObtenida != cantidadEsperada) {
            throw new IOException(
                    "Se esperaban " + cantidadEsperada
                            + " registros y se obtuvieron "
                            + cantidadObtenida);
        }
    }

    private Path crearTemporal(Set<Path> temporales) throws IOException {
        Path rutaTemporal = Files.createTempFile(
                rutaIndice.getParent(),
                "indice-merge-", ".tmp");
        temporales.add(rutaTemporal);
        return rutaTemporal;
    }

    private Path entregarResultado(Path resultado, Set<Path> temporales) {
        if (!temporales.remove(resultado)) {
            throw new IllegalStateException(
                    "El resultado no pertenece a los temporales del "
                            + "ordenamiento");
        }
        return resultado;
    }

    private void eliminarTemporal(Path temporal, Set<Path> temporales)
            throws IOException {
        Files.delete(temporal);
        temporales.remove(temporal);
    }

    private void limpiarTemporales(Set<Path> temporales, Throwable fallo) {
        for (Path temporal : Set.copyOf(temporales)) {
            try {
                Files.deleteIfExists(temporal);
                temporales.remove(temporal);
            } catch (IOException excepcionLimpieza) {
                fallo.addSuppressed(excepcionLimpieza);
            }
        }
    }

    private static long posicionBytes(long indice) {
        return indice * Constantes.TAMANO_ENTRADA_INDICE;
    }

    private static ArchivoRegistros<EntradaIndice> crearArchivoRegistros(
            Path ruta) {
        return new ArchivoRegistros<>(ruta, Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
