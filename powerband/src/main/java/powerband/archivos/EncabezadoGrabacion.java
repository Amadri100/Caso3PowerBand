package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Objects;

import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.util.Constantes;
import powerband.excepciones.ConstantesExcepciones;

public final class EncabezadoGrabacion {

    private final String nombre;
    private final long posicionInicial;
    private final long posicionFinal;
    private final long fechaEpochMillis;

    public EncabezadoGrabacion(String nombre, long posicionInicial,
            long posicionFinal, long fechaEpochMillis) {
        ValidacionesGenerales.validarNombre(nombre);
        if (posicionInicial < 0 || posicionFinal < posicionInicial) {
            throw ArgumentoInvalidoException.intervaloInvalido(
                    posicionInicial, posicionFinal);
        }

        this.nombre = nombre;
        this.posicionInicial = posicionInicial;
        this.posicionFinal = posicionFinal;
        this.fechaEpochMillis = fechaEpochMillis;
    }

    public String nombre() {
        return nombre;
    }

    public long posicionInicial() {
        return posicionInicial;
    }

    public long posicionFinal() {
        return posicionFinal;
    }

    public long fechaEpochMillis() {
        return fechaEpochMillis;
    }

    public byte[] serializar() {
        byte[] nombreBytes = ValidacionesGenerales.codificarNombre(nombre);
        ByteBuffer buffer = ByteBuffer.allocate(
                Constantes.TAMANO_ENCABEZADO_GRABACION)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        buffer.put(nombreBytes);
        // una lista de bytes en java se inicializa en 0
        buffer.put(new byte[Constantes.LONGITUD_MAXIMA_NOMBRE - buffer.position()]); 
        buffer.putLong(posicionInicial);
        buffer.putLong(posicionFinal);
        buffer.putLong(fechaEpochMillis);
        return buffer.array();
    }

    public static EncabezadoGrabacion desdeBytes(byte[] datos) {
        Objects.requireNonNull(datos, "datos");
        if (datos.length != Constantes.TAMANO_ENCABEZADO_GRABACION) {
            throw ArgumentoInvalidoException.detalle(
                    TipoErrorArgumento.TAMANO_REGISTRO_INVALIDO,
                    String.format(
                            ConstantesExcepciones.MSG_ENCABEZADO_LONGITUD,
                            Constantes.TAMANO_ENCABEZADO_GRABACION));
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        byte[] nombreBytes = new byte[Constantes.LONGITUD_MAXIMA_NOMBRE];
        buffer.get(nombreBytes);
        String nombre = ValidacionesGenerales.decodificarNombre(nombreBytes);
        long posicionInicial = buffer.getLong();
        long posicionFinal = buffer.getLong();
        long fechaEpochMillis = buffer.getLong();
        return new EncabezadoGrabacion(nombre, posicionInicial,
                posicionFinal, fechaEpochMillis);
    }

    @Override
    public String toString() {
        return "EncabezadoGrabacion{nombre='" + nombre + '\''
                + ", posicionInicial=" + posicionInicial
                + ", posicionFinal=" + posicionFinal
                + ", fechaEpochMillis=" + fechaEpochMillis + '}';
    }
}
