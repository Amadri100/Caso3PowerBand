package powerband.main;

import org.junit.jupiter.api.Test;

import powerband.controlador.Controlador;
import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void mainClassIsAvailable() {
        assertDoesNotThrow(() -> Class.forName("powerband.main.Main"));
    }

    @Test
    void creaLaFachadaConElControladorRealSingleton() {
        FachadaControlador fachada = Main.crearFachada();

        assertSame(Controlador.getInstance(), fachada);
    }

    @Test
    void reconoceElModoDeDemostracionDeSintetizadores() {
        assertTrue(Main.esDemostracionSintetizadores(
                new String[] {"--demo-synths"}));
        assertTrue(Main.esDemostracionSintetizadores(
                new String[] {"--DEMO-SYNTHS", "127.0.0.1", "57117"}));
        assertFalse(Main.esDemostracionSintetizadores(
                new String[] {}));
        assertFalse(Main.esDemostracionSintetizadores(
                new String[] {"PowerBand"}));
    }
}
