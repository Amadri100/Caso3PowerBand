package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Objects;

import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.util.Constantes;
import powerband.excepciones.ConstantesExcepciones;

public final class EntradaIndice {

    private final String nombre;
    private final long posicionInicial;
    private final long posicionFinal;

    public EntradaIndice(String nombre, long posicionInicial,
            long posicionFinal) {
        ValidacionesGenerales.validarNombre(nombre);
        if (posicionInicial < 0 || posicionFinal < posicionInicial) {
            throw ArgumentoInvalidoException.intervaloInvalido(
                    posicionInicial, posicionFinal);
        }

        this.nombre = nombre;
        this.posicionInicial = posicionInicial;
        this.posicionFinal = posicionFinal;
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

    public byte[] serializar() {
        byte[] nombreBytes = ValidacionesGenerales.codificarNombre(nombre);
        ByteBuffer buffer = ByteBuffer.allocate(
                Constantes.TAMANO_ENTRADA_INDICE)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        buffer.put(nombreBytes);
        buffer.put(new byte[
                Constantes.LONGITUD_MAXIMA_NOMBRE - nombreBytes.length]);
        buffer.putLong(posicionInicial);
        buffer.putLong(posicionFinal);
        return buffer.array();
    }

    public static EntradaIndice desdeBytes(byte[] datos) {
        Objects.requireNonNull(datos, "datos");
        if (datos.length != Constantes.TAMANO_ENTRADA_INDICE) {
            throw ArgumentoInvalidoException.detalle(
                    TipoErrorArgumento.TAMANO_REGISTRO_INVALIDO,
                    String.format(
                            ConstantesExcepciones.MSG_ENTRADA_INDICE_LONGITUD,
                            Constantes.TAMANO_ENTRADA_INDICE));
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
        byte[] nombreBytes = new byte[Constantes.LONGITUD_MAXIMA_NOMBRE];
        buffer.get(nombreBytes);
        String nombre = ValidacionesGenerales.decodificarNombre(nombreBytes);
        long posicionInicial = buffer.getLong();
        long posicionFinal = buffer.getLong();
        return new EntradaIndice(nombre, posicionInicial, posicionFinal);
    }

    @Override
    public String toString() {
        return "EntradaIndice{nombre='" + nombre + '\''
                + ", posicionInicial=" + posicionInicial
                + ", posicionFinal=" + posicionFinal + '}';
    }
}
