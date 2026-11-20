package powerband.archivos;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

public final class ListaGrabaciones {

    private final Path ruta;

    public ListaGrabaciones(Path ruta) {
        this.ruta = Objects.requireNonNull(ruta, "ruta")
                .toAbsolutePath().normalize();
    }

    public void agregar(EntradaListaGrabacion nuevaEntrada)
            throws IOException {
        Objects.requireNonNull(nuevaEntrada, "nuevaEntrada");
        Path rutaTemporal = null;

        try {
            rutaTemporal = Files.createTempFile(
                    ruta.getParent(), ruta.getFileName() + ".", ".tmp");
            try (ArchivoRegistros<EntradaListaGrabacion> archivo =
                        crearArchivoRegistros(ruta);
                    ArchivoRegistros<EntradaListaGrabacion> temporal =
                        crearArchivoRegistros(rutaTemporal)) {
                long cantidad = archivo.cantidadRegistros();
                long posicionInsercion = encontrarPosicionInsercion(
                        archivo, nuevaEntrada, cantidad);
                copiarConInsercion(archivo, temporal, nuevaEntrada, cantidad,
                        posicionInsercion);
                validarListaOrdenada(temporal, cantidad + 1);
            }
            reemplazarArchivo(rutaTemporal);
            rutaTemporal = null;
        } catch (IOException excepcion) {
            IOException contextual = informarFallo(
                    "agregar una grabacion a recordings.list", excepcion);
            eliminarTemporal(rutaTemporal, contextual);
            throw contextual;
        } catch (RuntimeException excepcion) {
            eliminarTemporal(rutaTemporal, excepcion);
            throw excepcion;
        }
    }

    public List<EntradaListaGrabacion> listar() throws IOException {
        try (ArchivoRegistros<EntradaListaGrabacion> archivo =
                crearArchivoRegistros(ruta)) {
            long cantidad = archivo.cantidadRegistros();
            List<EntradaListaGrabacion> entradas = new ArrayList<>();
            for (long indice = 0; indice < cantidad; indice++) {
                entradas.add(archivo.leer(posicionBytes(indice)));
            }
            return List.copyOf(entradas);
        }
    }

    private long encontrarPosicionInsercion(
            ArchivoRegistros<EntradaListaGrabacion> archivo,
            EntradaListaGrabacion nuevaEntrada, long cantidad)
            throws IOException {
        long posicionInsercion = cantidad;
        for (long indice = 0; indice < cantidad; indice++) {
            EntradaListaGrabacion existente =
                    archivo.leer(posicionBytes(indice));
            if (ComparadorNombres.INSTANCIA.compare(
                    existente.nombre(), nuevaEntrada.nombre()) == 0) {
                String mensaje = "Nombre de grabacion duplicado: "
                        + nuevaEntrada.nombre();
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        mensaje, ruta.toString());
                throw new IllegalArgumentException(mensaje);
            }
            if (posicionInsercion == cantidad
                    && compararEntradas(existente, nuevaEntrada) > 0) {
                posicionInsercion = indice;
            }
        }
        return posicionInsercion;
    }

    private void copiarConInsercion(
            ArchivoRegistros<EntradaListaGrabacion> archivo,
            ArchivoRegistros<EntradaListaGrabacion> destino,
            EntradaListaGrabacion nuevaEntrada, long cantidad,
            long posicionInsercion) throws IOException {
        long posicionDestino = 0;
        boolean insertada = false;
        for (long indice = 0; indice < cantidad; indice++) {
            if (!insertada && indice == posicionInsercion) {
                destino.escribir(posicionDestino, nuevaEntrada);
                posicionDestino += Constantes.TAMANO_ENTRADA_LISTA_GRABACION;
                insertada = true;
            }
            destino.escribir(posicionDestino, archivo.leer(posicionBytes(indice)));
            posicionDestino += Constantes.TAMANO_ENTRADA_LISTA_GRABACION;
        }
        if (!insertada) {
            destino.escribir(posicionDestino, nuevaEntrada);
        }
    }

    private void validarListaOrdenada(
            ArchivoRegistros<EntradaListaGrabacion> listaTemporal,
            long cantidadEsperada) throws IOException {
        if (listaTemporal.cantidadRegistros() != cantidadEsperada) {
            throw new IOException(
                    "La lista temporal tiene una cantidad inesperada "
                            + "de registros");
        }

        EntradaListaGrabacion anterior = null;
        for (long indice = 0; indice < cantidadEsperada; indice++) {
            EntradaListaGrabacion actual =
                    listaTemporal.leer(posicionBytes(indice));
            if (anterior != null && compararEntradas(anterior, actual) >= 0) {
                throw new IOException(
                        "La lista temporal no está ordenada o contiene "
                                + "nombres duplicados");
            }
            anterior = actual;
        }
    }

    private void reemplazarArchivo(Path rutaTemporal) throws IOException {
        try {
            Files.move(rutaTemporal, ruta,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException excepcion) {
            throw new IOException(
                    "El sistema de archivos no permite reemplazar "
                            + "recordings.list de forma atomica",
                    excepcion);
        }
    }

    private void eliminarTemporal(Path rutaTemporal, Throwable fallo) {
        if (rutaTemporal == null) {
            return;
        }
        try {
            Files.deleteIfExists(rutaTemporal);
        } catch (IOException excepcionLimpieza) {
            fallo.addSuppressed(excepcionLimpieza);
        }
    }

    private IOException informarFallo(String operacion,
            IOException excepcion) {
        String mensaje = "No se pudo " + operacion + " en " + ruta
                + ": " + excepcion.getMessage();
        LoggerPowerBand.getInstance().registrarErrorArchivo(
                mensaje, ruta.toString());
        return new IOException(mensaje, excepcion);
    }

    private static int compararEntradas(EntradaListaGrabacion entrada1,
            EntradaListaGrabacion entrada2) {
        int comparacionFecha = Long.compare(
                entrada1.fechaEpochMillis(), entrada2.fechaEpochMillis());
        if (comparacionFecha != 0) {
            return comparacionFecha;
        }
        return ComparadorNombres.INSTANCIA.compare(
                entrada1.nombre(), entrada2.nombre());
    }

    private static long posicionBytes(long indice) {
        return indice * Constantes.TAMANO_ENTRADA_LISTA_GRABACION;
    }

    private static ArchivoRegistros<EntradaListaGrabacion>
            crearArchivoRegistros(Path ruta) {
        return new ArchivoRegistros<>(ruta,
                Constantes.TAMANO_ENTRADA_LISTA_GRABACION,
                EntradaListaGrabacion::desdeBytes,
                EntradaListaGrabacion::serializar);
    }
}
