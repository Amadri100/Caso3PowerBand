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
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.util.Constantes;
import powerband.excepciones.ArchivoException;
import powerband.excepciones.CantidadInesperadaException;
import powerband.excepciones.NombreDuplicadoException;
import powerband.excepciones.OrdenamientoException;
import powerband.excepciones.tipos.TipoErrorArchivo;
import powerband.excepciones.tipos.TipoErrorOrdenamiento;

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
        } catch (ArchivoException excepcion) {
            eliminarTemporal(rutaTemporal, excepcion);
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    excepcion.getMessage(), ruta.toString(), OrigenLog.ARCHIVOS, excepcion));
            throw excepcion;
        } catch (IOException excepcion) {
            ArchivoException fallo = ArchivoException.errorES(
                    ruta.toString(), "agregar una grabación",
                    excepcion);
            eliminarTemporal(rutaTemporal, fallo);
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    fallo.getMessage(), ruta.toString(), OrigenLog.ARCHIVOS, fallo));
            throw fallo;
        } catch (RuntimeException excepcion) {
            eliminarTemporal(rutaTemporal, excepcion);
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    excepcion.getMessage(), ruta.toString(), OrigenLog.ARCHIVOS, excepcion));
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
                throw new NombreDuplicadoException(
                        existente.nombre(), nuevaEntrada.nombre(),
                        ruta.toString());
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
            throw new CantidadInesperadaException(
                    ruta.toString(),
                    cantidadEsperada,
                    listaTemporal.cantidadRegistros());
        }

        EntradaListaGrabacion anterior = null;
        for (long indice = 0; indice < cantidadEsperada; indice++) {
            EntradaListaGrabacion actual =
                    listaTemporal.leer(posicionBytes(indice));
            if (anterior != null && compararEntradas(anterior, actual) >= 0) {
                throw new OrdenamientoException(
                        TipoErrorOrdenamiento.INDICE_DESORDENADO,
                        ruta.toString(), actual.nombre());
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
            throw new ArchivoException(
                    TipoErrorArchivo.REEMPLAZO_NO_ATOMICO,
                    ruta.toString(),
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
