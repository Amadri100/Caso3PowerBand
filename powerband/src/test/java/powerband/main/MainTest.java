package powerband.main;

import org.junit.jupiter.api.Test;

import powerband.controlador.Controlador;
import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
