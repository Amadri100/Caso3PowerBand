package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;


//runs = una parte ordenada
public final class OrdenadorIndice {

    private final Path rutaIndice;

    public OrdenadorIndice(Path rutaIndice) {
        this.rutaIndice = Objects.requireNonNull(rutaIndice, "rutaIndice")
                .toAbsolutePath().normalize();
    }

    public void reemplazarResultado(Path rutaResultado) throws IOException {
        new ReemplazadorIndice(rutaIndice, rutaResultado).reemplazar();
    }

    public void ordenarYReemplazar() throws IOException {
        Path resultado = ordenar();
        try {
            reemplazarResultado(resultado);
        } catch (IOException | RuntimeException fallo) {
            try {
                Files.deleteIfExists(resultado);
            } catch (IOException limpieza) {
                fallo.addSuppressed(limpieza);
            }
            throw fallo;
        }
        try {
            Files.deleteIfExists(resultado);
        } catch (IOException limpieza) {
            throw limpieza;
        }
    }

    //Algoritmo que ordena el archivo de indice como tal
    public Path ordenar() throws IOException {
        //Caminoss a los archivos temporales
        Set<Path> temporales = new LinkedHashSet<>();
        try {
            //Abre el archvio de indice
            long cantidad;
            try (ArchivoRegistros<EntradaIndice> archivoIndice =
                    crearArchivoRegistros(rutaIndice)) {
                cantidad = archivoIndice.cantidadRegistros();
            }
            //Primero separa todo en runs de 1 
            Path origenOrdenado = rutaIndice;
            long longitudRun = 1;
            //si hay un solo registro o menos ya esta ordenado
            if (cantidad <= 1) {
                Path salida = crearTemporal(temporales);
                copiar(origenOrdenado, salida, cantidad);
                validarResultado(salida, cantidad);
                return entregarResultado(salida, temporales);
            }
            //Termina si la longitud de la run alcanza la cantidad de registros
            while (longitudRun < cantidad) {
                Path entradaIzquierda = crearTemporal(temporales);
                Path entradaDerecha = crearTemporal(temporales);
                Path salida = crearTemporal(temporales);
                //Distribuye las runs en una logitud especifica, por el orden 
                //del procedimiento siempre estan ordenados.
                distribuirRuns(origenOrdenado, entradaIzquierda,
                        entradaDerecha, cantidad, longitudRun);
                //Mezcla la distribución de runs en una forma de que quede ordenado
                mezclarRuns(entradaIzquierda, entradaDerecha, salida,
                        longitudRun);
                //Valida que este ordenado
                validarCantidad(salida, cantidad);
                
                //Limpia los archivos
                eliminarTemporal(entradaIzquierda, temporales);
                eliminarTemporal(entradaDerecha, temporales);
                if (!origenOrdenado.equals(rutaIndice)) {
                    eliminarTemporal(origenOrdenado, temporales);
                }

                origenOrdenado = salida;
                //Evita que se la longitud de la run supero a cantidad. 
                //En algunos casos como tener 7 registros, la formula se pasa.
                longitudRun = longitudRun > cantidad / 2
                        ? cantidad : longitudRun * 2;
            }
            //Validación final
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
    //Distribuye las runs en partes de izquierda y derecha
    private void distribuirRuns(Path origen, Path rutaIzquierda,
            Path rutaDerecha, long cantidad, long longitudRun)
            throws IOException {
        //Archivos
        ArchivoRegistros<EntradaIndice> archivoOrigen =
                crearArchivoRegistros(origen);
        ArchivoRegistros<EntradaIndice> izquierda =
                crearArchivoRegistros(rutaIzquierda);
        ArchivoRegistros<EntradaIndice> derecha =
                crearArchivoRegistros(rutaDerecha);
        //archivoOrigen, etc... lo cierra autoamticamente
        try (archivoOrigen; izquierda; derecha) {
            long indiceOrigen = 0;
            long cantidadIzquierda = 0;
            long cantidadDerecha = 0;
            long numeroRun = 0;
            while (indiceOrigen < cantidad) {
                //Decide entre tomar el tamaño definido de la run o el tamaño real si fuera el caso
                //Toma el minimo
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
    }

    //Mezcla las runs ordenadas.
    //Error si hay nombres duplicados
    private void mezclarRuns(Path rutaIzquierda, Path rutaDerecha,
            Path rutaSalida, long longitudRun) throws IOException {
        ArchivoRegistros<EntradaIndice> izquierda =
                crearArchivoRegistros(rutaIzquierda);
        ArchivoRegistros<EntradaIndice> derecha =
                crearArchivoRegistros(rutaDerecha);
        ArchivoRegistros<EntradaIndice> salida =
                crearArchivoRegistros(rutaSalida);
        // Cierra los tres archivos automáticamente al terminar
        try (izquierda; derecha; salida) {
            long cantidadIzquierda = izquierda.cantidadRegistros();
            long cantidadDerecha = derecha.cantidadRegistros();
            // Posición actual de lectura en cada entrada y de escritura en la salida
            long indiceIzquierda = 0;
            long indiceDerecha = 0;
            long indiceSalida = 0;

            // Cada vuelta procesa un par de runs (uno de cada archivo)
            while (indiceIzquierda < cantidadIzquierda
                    || indiceDerecha < cantidadDerecha) {
                // Fin (exclusivo) de cada run. Math.min evita pasarse del
                // final del archivo cuando el último run es más corto
                long finRunIzquierdo = Math.min(
                        indiceIzquierda + longitudRun, cantidadIzquierda);
                long finRunDerecho = Math.min(
                        indiceDerecha + longitudRun, cantidadDerecha);

                //Inicia el algoritmo de mezcla
                while (indiceIzquierda < finRunIzquierdo
                        || indiceDerecha < finRunDerecho) {
                    //null si no hay más, guarda lo datos
                    EntradaIndice entradaIzquierda =
                            indiceIzquierda < finRunIzquierdo
                                    ? izquierda.leer(
                                            posicionBytes(indiceIzquierda))
                                    : null;
                    EntradaIndice entradaDerecha =
                            indiceDerecha < finRunDerecho
                                    ? derecha.leer(
                                            posicionBytes(indiceDerecha))
                                    : null;

                    //Comparación segun estado; evita crear otro for
                    int comparacion;
                    if (entradaIzquierda == null) {
                        comparacion = 1;
                    } else if (entradaDerecha == null) {
                        comparacion = -1;
                    } else {
                        comparacion = ComparadorNombres.INSTANCIA.compare(
                                entradaIzquierda.nombre(),
                                entradaDerecha.nombre());
                    }

                    // Empate según el comparador: "Ana" y "ana" son duplicados
                    // (aquí ambas entradas existen, no hay nulls)
                    if (comparacion == 0) {
                        throw new IOException(
                                "El índice contiene nombres duplicados: "
                                        + entradaIzquierda.nombre() + " / "
                                        + entradaDerecha.nombre());
                    }

                    if (comparacion < 0) {
                        // El menor está a la izquierda
                        salida.escribir(posicionBytes(indiceSalida++),
                                entradaIzquierda);
                        indiceIzquierda++;
                    } else {
                        // El menor está a la derecha (o la izquierda se agotó)
                        salida.escribir(posicionBytes(indiceSalida++),
                                entradaDerecha);
                        indiceDerecha++;
                    }
                }
            }
        }
    }
    //Copia una cantidad de registros de Origen -> Destino
    private void copiar(Path rutaOrigen, Path rutaDestino, long cantidad)
            throws IOException {
        ArchivoRegistros<EntradaIndice> origen =
                crearArchivoRegistros(rutaOrigen);
        ArchivoRegistros<EntradaIndice> destino =
                crearArchivoRegistros(rutaDestino);
        try (origen; destino) {
            for (long indice = 0; indice < cantidad; indice++) {
                destino.escribir(posicionBytes(indice),
                        origen.leer(posicionBytes(indice)));
            }
        }
    }
    //Determina si el resultado es correcto (validarCantidad + Ordenado correctamente)
    private void validarResultado(Path rutaResultado, long cantidadEsperada)
            throws IOException {
        validarCantidad(rutaResultado, cantidadEsperada);
        ArchivoRegistros<EntradaIndice> resultado =
                crearArchivoRegistros(rutaResultado);
        
        try (resultado) {
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
    }
    //Revisa si la cantidad de registros real coincide con la esperada
    private void validarCantidad(Path ruta, long cantidadEsperada)
            throws IOException {
        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoRegistros(ruta)) {
            long cantidadObtenida = archivo.cantidadRegistros();
            if (cantidadObtenida != cantidadEsperada) {
                throw new IOException(
                        "Se esperaban " + cantidadEsperada
                                + " registros y se obtuvieron "
                                + cantidadObtenida);
            }
        }
    }
    //Crea un archivo temporal
    private Path crearTemporal(Set<Path> temporales) throws IOException {
        Path rutaTemporal = Files.createTempFile(
                rutaIndice.getParent(),
                "indice-merge-", ".tmp");
        temporales.add(rutaTemporal);
        return rutaTemporal;
    }
    //Entrega el resultado si estaba en los temporales.
    private Path entregarResultado(Path resultado, Set<Path> temporales) {
        //Si no existia el elemento se da un error
        if (!temporales.remove(resultado)) {
            throw new IllegalStateException(
                    "El resultado no pertenece a los temporales del "
                            + "ordenamiento");
        }
        return resultado;
    }
    //Elimina el archivo temporal sin revisar
    private void eliminarTemporal(Path temporal, Set<Path> temporales)
            throws IOException {
        Files.delete(temporal);
        temporales.remove(temporal);
    }
    //Elimina los achivos temporales
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
    //Convierte de del numero de indice a su posicion en bytes
    private static long posicionBytes(long indice) {
        return indice * Constantes.TAMANO_ENTRADA_INDICE;
    }
    //Cra un registro del tamaño de una entrada de indice
    private static ArchivoRegistros<EntradaIndice> crearArchivoRegistros(
            Path ruta) {
        return new ArchivoRegistros<>(ruta, Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
