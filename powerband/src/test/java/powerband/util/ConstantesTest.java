package powerband.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantesTest {

    @Test
    void constantesExponenConfiguracionYRegistros() {
        assertEquals("127.0.0.1", Constantes.HOST_DEFECTO);
        assertEquals(57117, Constantes.PUERTO_DEFECTO);
        assertEquals("powerband/logs", Constantes.RUTA_LOGS);
        assertEquals("comando", Constantes.CATEGORIA_COMANDO);
        assertEquals("info", Constantes.CATEGORIA_INFO);
        assertEquals("error", Constantes.CATEGORIA_ERROR);
        assertEquals(200, Constantes.LONGITUD_MAXIMA_NOMBRE);
        assertEquals(224, Constantes.TAMANO_ENCABEZADO_GRABACION);
        assertEquals(75, Constantes.TAMANO_COMANDO_GRABADO);
        assertEquals(216, Constantes.TAMANO_ENTRADA_INDICE);
    }

    @Test
    void claseEsFinalYConstructorEsPrivado() throws Exception {
        assertTrue(Modifier.isFinal(Constantes.class.getModifiers()));
        Constructor<Constantes> constructor =
                Constantes.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
}
