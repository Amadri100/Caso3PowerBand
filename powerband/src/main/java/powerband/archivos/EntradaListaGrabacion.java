package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Objects;

import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.util.Constantes;
import powerband.excepciones.ConstantesExcepciones;

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
            throw ArgumentoInvalidoException.detalle(
                    TipoErrorArgumento.TAMANO_REGISTRO_INVALIDO,
                    String.format(
                            ConstantesExcepciones.MSG_ENTRADA_LISTA_LONGITUD,
                            Constantes.TAMANO_ENTRADA_LISTA_GRABACION));
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
