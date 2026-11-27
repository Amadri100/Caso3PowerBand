package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import powerband.excepciones.ArgumentoInvalidoException;
import powerband.util.Constantes;

public interface ValidacionesGenerales {

    public static void validarNombre(String nombre) {
        byte[] bytes = codificarNombre(nombre);
        if (bytes.length == 0
                || bytes.length > Constantes.LONGITUD_MAXIMA_NOMBRE) {
            throw ArgumentoInvalidoException.nombreInvalido();
        }
        if (nombre.indexOf('\0') >= 0) {
            throw ArgumentoInvalidoException.nombreConNulos();
        }
    }

    public static byte[] codificarNombre(String nombre) {
        if (nombre == null) {
            throw ArgumentoInvalidoException.nombreNulo();
        }
        try {
            ByteBuffer codificado = StandardCharsets.UTF_8.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(nombre));
            byte[] bytes = new byte[codificado.remaining()];
            codificado.get(bytes);
            return bytes;
        } catch (CharacterCodingException excepcion) {
            throw ArgumentoInvalidoException.nombreUtf8Invalido(excepcion);
        }
    }

    public static String decodificarNombre(byte[] bytes) {
        int longitud = 0;
        while (longitud < bytes.length && bytes[longitud] != 0) {
            longitud++;
        }
        for (int indice = longitud; indice < bytes.length; indice++) {
            if (bytes[indice] != 0) {
                throw ArgumentoInvalidoException.rellenoNombreInvalido();
            }
        }
        if (longitud == 0) {
            throw ArgumentoInvalidoException.nombreVacio();
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, 0, longitud))
                    .toString();
        } catch (CharacterCodingException excepcion) {
            throw ArgumentoInvalidoException.nombreCodificadoInvalido(excepcion);
        }
    }
}
