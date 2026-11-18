package powerband.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracionesTest {

    @Test
    void usaValoresPorDefecto() {
        Configuraciones configuraciones = new Configuraciones();

        assertEquals(Constantes.HOST_DEFECTO, configuraciones.getHost());
        assertEquals(Constantes.PUERTO_DEFECTO, configuraciones.getPuerto());
    }

    @Test
    void permiteCambiarHostPuertoYOpciones() {
        Configuraciones configuraciones = new Configuraciones();

        configuraciones.setHost("localhost");
        configuraciones.setPuerto(9000);
        configuraciones.setOpcion("modo", "pruebas");

        assertEquals("localhost", configuraciones.getHost());
        assertEquals(9000, configuraciones.getPuerto());
        assertEquals("pruebas", configuraciones.getOpcion("modo"));
        assertTrue(configuraciones.getOpcionesModificables().containsKey("modo"));
    }

    @Test
    void rechazaPuertoFueraDeRango() {
        Configuraciones configuraciones = new Configuraciones();

        assertThrows(IllegalArgumentException.class,
                () -> configuraciones.setPuerto(0));
        assertThrows(IllegalArgumentException.class,
                () -> configuraciones.setPuerto(65536));
    }

    @Test
    void exponeUnaInstanciaCompartida() {
        assertSame(Configuraciones.getInstance(),
                Configuraciones.obtenerInstancia());
    }
}
