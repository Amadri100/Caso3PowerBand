package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import powerband.util.Constantes;

public final class EntradaIndice {

    private final String nombre;
    private final long posicionInicial;
    private final long posicionFinal;

    public EntradaIndice(String nombre, long posicionInicial,
            long posicionFinal) {
        validarNombre(nombre);
        if (posicionInicial < 0 || posicionFinal < posicionInicial) {
            throw new IllegalArgumentException(
                    "El intervalo del indice no es valido");
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
        byte[] nombreBytes = codificarNombre(nombre);
        ByteBuffer buffer = ByteBuffer.allocate(
                Constantes.TAMANO_ENTRADA_INDICE)
                .order(ByteOrder.BIG_ENDIAN);
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
            throw new IllegalArgumentException(
                    "La entrada de indice debe tener "
                            + Constantes.TAMANO_ENTRADA_INDICE
                            + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);
        byte[] nombreBytes = new byte[Constantes.LONGITUD_MAXIMA_NOMBRE];
        buffer.get(nombreBytes);
        String nombre = decodificarNombre(nombreBytes);
        long posicionInicial = buffer.getLong();
        long posicionFinal = buffer.getLong();
        return new EntradaIndice(nombre, posicionInicial, posicionFinal);
    }

    private static void validarNombre(String nombre) {
        byte[] bytes = codificarNombre(nombre);
        if (bytes.length == 0
                || bytes.length > Constantes.LONGITUD_MAXIMA_NOMBRE) {
            throw new IllegalArgumentException(
                    "El nombre debe ocupar entre 1 y "
                            + Constantes.LONGITUD_MAXIMA_NOMBRE
                            + " bytes UTF-8");
        }
        if (nombre.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(
                    "El nombre no puede contener bytes nulos");
        }
    }

    private static byte[] codificarNombre(String nombre) {
        Objects.requireNonNull(nombre, "nombre");
        try {
            ByteBuffer codificado = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(nombre));
            byte[] bytes = new byte[codificado.remaining()];
            codificado.get(bytes);
            return bytes;
        } catch (CharacterCodingException excepcion) {
            throw new IllegalArgumentException(
                    "El nombre no es una cadena UTF-8 valida", excepcion);
        }
    }

    private static String decodificarNombre(byte[] bytes) {
        int longitud = 0;
        while (longitud < bytes.length && bytes[longitud] != 0) {
            longitud++;
        }
        for (int indice = longitud; indice < bytes.length; indice++) {
            if (bytes[indice] != 0) {
                throw new IllegalArgumentException(
                        "El relleno del nombre debe contener ceros");
            }
        }
        if (longitud == 0) {
            throw new IllegalArgumentException("El nombre no puede estar vacio");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, 0, longitud))
                    .toString();
        } catch (CharacterCodingException excepcion) {
            throw new IllegalArgumentException(
                    "El nombre del indice no es UTF-8 valido",
                    excepcion);
        }
    }

    @Override
    public String toString() {
        return "EntradaIndice{nombre='" + nombre + '\''
                + ", posicionInicial=" + posicionInicial
                + ", posicionFinal=" + posicionFinal + '}';
    }
}
