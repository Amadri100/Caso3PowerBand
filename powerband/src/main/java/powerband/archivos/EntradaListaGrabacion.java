package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Objects;

import powerband.util.Constantes;

public final class EntradaListaGrabacion {

    private final String nombre;
    private final long fechaEpochMillis;

    public EntradaListaGrabacion(String nombre, long fechaEpochMillis) {
        ValidacionesGenerales.validarNombre(nombre);
        this.nombre = nombre;
        this.fechaEpochMillis = fechaEpochMillis;
    }

    public String nombre() {
        return nombre;
    }

    public long fechaEpochMillis() {
        return fechaEpochMillis;
    }

    public byte[] serializar() {
        byte[] nombreBytes = ValidacionesGenerales.codificarNombre(nombre);
        ByteBuffer buffer = ByteBuffer.allocate(
                Constantes.TAMANO_ENTRADA_LISTA_GRABACION)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        buffer.put(nombreBytes);
        buffer.put(new byte[
                Constantes.LONGITUD_MAXIMA_NOMBRE - nombreBytes.length]);
        buffer.putLong(fechaEpochMillis);
        return buffer.array();
    }

    public static EntradaListaGrabacion desdeBytes(byte[] datos) {
        Objects.requireNonNull(datos, "datos");
        if (datos.length != Constantes.TAMANO_ENTRADA_LISTA_GRABACION) {
            throw new IllegalArgumentException(
                    "La entrada de lista debe tener "
                            + Constantes.TAMANO_ENTRADA_LISTA_GRABACION
                            + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        byte[] nombreBytes = new byte[Constantes.LONGITUD_MAXIMA_NOMBRE];
        buffer.get(nombreBytes);
        String nombre = ValidacionesGenerales.decodificarNombre(nombreBytes);
        long fechaEpochMillis = buffer.getLong();
        return new EntradaListaGrabacion(nombre, fechaEpochMillis);
    }

    @Override
    public String toString() {
        return "EntradaListaGrabacion{nombre='" + nombre + '\''
                + ", fechaEpochMillis=" + fechaEpochMillis + '}';
    }
}
