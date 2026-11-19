package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncabezadoGrabacionTest {

    @Test
    void serializaYLeeUnRegistroBigEndianDeLongitudFija() {
        EncabezadoGrabacion encabezado =
                new EncabezadoGrabacion("sesion", 224, 672, 1_700_000_000L);

        byte[] datos = encabezado.serializar();
        EncabezadoGrabacion leido = EncabezadoGrabacion.desdeBytes(datos);

        assertEquals(224, datos.length);
        assertEquals("sesion", leido.nombre());
        assertEquals(224, leido.posicionInicial());
        assertEquals(672, leido.posicionFinal());
        assertEquals(1_700_000_000L, leido.fechaEpochMillis());
        assertEquals(224, ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN).getLong(200));
        assertTrue(leido.toString().contains("sesion"));
    }

    @Test
    void admiteLimiteUtf8YRechazaNombresSobreLimite() {
        String nombreDe200Bytes = "a".repeat(197) + "€";
        String nombreDe201Bytes = "a".repeat(198) + "€";

        assertEquals(nombreDe200Bytes,
                EncabezadoGrabacion.desdeBytes(
                        new EncabezadoGrabacion(
                                nombreDe200Bytes, 0, 0, 0).serializar())
                        .nombre());
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion(nombreDe201Bytes, 0, 0, 0));
    }

    @Test
    void rechazaIntervalosNombresYRegistrosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion("", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion("nombre\0invalido", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion("\uD800", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion("sesion", -1, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EncabezadoGrabacion("sesion", 10, 9, 0));
        assertThrows(IllegalArgumentException.class,
                () -> EncabezadoGrabacion.desdeBytes(new byte[223]));
    }

    @Test
    void rechazaUtf8MalFormadoYRellenoNoCero() {
        byte[] utf8Invalido =
                new EncabezadoGrabacion("x", 0, 0, 0).serializar();
        utf8Invalido[0] = (byte) 0xC3;
        assertThrows(IllegalArgumentException.class,
                () -> EncabezadoGrabacion.desdeBytes(utf8Invalido));

        byte[] rellenoInvalido =
                new EncabezadoGrabacion("x", 0, 0, 0).serializar();
        rellenoInvalido[2] = 1;
        assertThrows(IllegalArgumentException.class,
                () -> EncabezadoGrabacion.desdeBytes(rellenoInvalido));
    }
}
