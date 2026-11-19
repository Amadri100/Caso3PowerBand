package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import powerband.util.Constantes;

public interface ValidacionesGenerales {

    static void validarNombre(String nombre) {
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

    static byte[] codificarNombre(String nombre) {
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

    static String decodificarNombre(byte[] bytes) {
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
                    "El nombre codificado no es UTF-8 valido",
                    excepcion);
        }
    }
}
