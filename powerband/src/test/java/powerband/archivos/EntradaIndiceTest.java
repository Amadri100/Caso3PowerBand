package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaIndiceTest {

    @Test
    void serializaYLeeUnRegistroBigEndianDeLongitudFija() {
        EntradaIndice entrada =
                new EntradaIndice("sesion", 224, 672);

        byte[] datos = entrada.serializar();
        EntradaIndice leida = EntradaIndice.desdeBytes(datos);

        assertEquals(216, datos.length);
        assertEquals("sesion", leida.nombre());
        assertEquals(224, leida.posicionInicial());
        assertEquals(672, leida.posicionFinal());
        assertEquals(224, ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN).getLong(200));
        assertEquals(672, ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN).getLong(208));
        assertTrue(leida.toString().contains("sesion"));
    }

    @Test
    void admiteLimiteUtf8YRechazaNombresSobreLimite() {
        String nombreDe200Bytes = "a".repeat(197) + "€";
        String nombreDe201Bytes = "a".repeat(198) + "€";

        assertEquals(nombreDe200Bytes,
                EntradaIndice.desdeBytes(
                        new EntradaIndice(
                                nombreDe200Bytes, 0, 0).serializar())
                        .nombre());
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice(nombreDe201Bytes, 0, 0));
    }

    @Test
    void rechazaIntervalosNombresYRegistrosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice("", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice("nombre\0invalido", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice("\uD800", 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice("sesion", -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaIndice("sesion", 10, 9));
        assertThrows(IllegalArgumentException.class,
                () -> EntradaIndice.desdeBytes(new byte[215]));
    }

    @Test
    void rechazaUtf8MalFormadoYRellenoNoCero() {
        byte[] utf8Invalido =
                new EntradaIndice("x", 0, 0).serializar();
        utf8Invalido[0] = (byte) 0xC3;
        assertThrows(IllegalArgumentException.class,
                () -> EntradaIndice.desdeBytes(utf8Invalido));

        byte[] rellenoInvalido =
                new EntradaIndice("x", 0, 0).serializar();
        rellenoInvalido[2] = 1;
        assertThrows(IllegalArgumentException.class,
                () -> EntradaIndice.desdeBytes(rellenoInvalido));
    }
}
