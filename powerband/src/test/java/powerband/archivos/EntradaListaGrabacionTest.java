package powerband.archivos;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaListaGrabacionTest {

    @Test
    void serializaYLeeUnRegistroFijoDe208Bytes() {
        EntradaListaGrabacion entrada =
                new EntradaListaGrabacion("sesion", 1_700_000_000L);

        byte[] datos = entrada.serializar();
        EntradaListaGrabacion leida =
                EntradaListaGrabacion.desdeBytes(datos);

        assertEquals(Constantes.TAMANO_ENTRADA_LISTA_GRABACION, datos.length);
        assertEquals("sesion", leida.nombre());
        assertEquals(1_700_000_000L, leida.fechaEpochMillis());
        assertEquals(1_700_000_000L,
                ByteBuffer.wrap(datos)
                        .order(Constantes.ORDEN_BYTES_PROGRAMA).getLong(200));
        assertTrue(leida.toString().contains("sesion"));
    }

    @Test
    void validaNombreUtf8YLongitudDelRegistro() {
        String nombreDe200Bytes = "a".repeat(197) + "€";
        String nombreDe201Bytes = "a".repeat(198) + "€";

        assertEquals(nombreDe200Bytes,
                EntradaListaGrabacion.desdeBytes(
                        new EntradaListaGrabacion(nombreDe200Bytes, 0)
                                .serializar())
                        .nombre());
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaListaGrabacion(nombreDe201Bytes, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new EntradaListaGrabacion("", 0));
        assertThrows(IllegalArgumentException.class,
                () -> EntradaListaGrabacion.desdeBytes(new byte[207]));
    }

    @Test
    void rechazaUtf8YRellenoInvalidos() {
        byte[] utf8Invalido =
                new EntradaListaGrabacion("x", 0).serializar();
        utf8Invalido[0] = (byte) 0xC3;
        assertThrows(IllegalArgumentException.class,
                () -> EntradaListaGrabacion.desdeBytes(utf8Invalido));

        byte[] rellenoInvalido =
                new EntradaListaGrabacion("x", 0).serializar();
        rellenoInvalido[2] = 1;
        assertThrows(IllegalArgumentException.class,
                () -> EntradaListaGrabacion.desdeBytes(rellenoInvalido));
    }
}
