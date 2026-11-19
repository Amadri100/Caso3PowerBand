package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Objects;

import powerband.util.Constantes;

public final class EncabezadoGrabacion {

    private final String nombre;
    private final long posicionInicial;
    private final long posicionFinal;
    private final long fechaEpochMillis;

    public EncabezadoGrabacion(String nombre, long posicionInicial,
            long posicionFinal, long fechaEpochMillis) {
        ValidacionesGenerales.validarNombre(nombre);
        if (posicionInicial < 0 || posicionFinal < posicionInicial) {
            throw new IllegalArgumentException(
                    "El intervalo de la grabacion no es valido");
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
            throw new IllegalArgumentException(
                    "El encabezado debe tener "
                            + Constantes.TAMANO_ENCABEZADO_GRABACION
                            + " bytes");
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
