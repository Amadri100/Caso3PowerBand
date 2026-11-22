package powerband.input;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @Test
    void normalizaUnValorEntreRangosMusicales() {
        double resultado = NormalizadorEntrada.normalizar(
                50.0, 0.0, 100.0, 0.0, 1.0);

        assertEquals(0.5, resultado);
        assertThrows(IllegalArgumentException.class,
                () -> NormalizadorEntrada.normalizar(
                        101.0, 0.0, 100.0, 0.0, 1.0));
    }

    @Test
    void iniciaYDetieneCadaEntradaIndependientemente() throws InterruptedException {
        EntradaDePrueba primera = new EntradaDePrueba("primera");
        EntradaDePrueba segunda = new EntradaDePrueba("segunda");

        primera.iniciar();
        segunda.iniciar();
        primera.detener();

        assertFalse(primera.estaActiva());
        assertTrue(segunda.estaActiva());

        segunda.detener();
        assertFalse(segunda.estaActiva());
    }

    @Test
    void rechazaValoresFueraDeRango() {
        EntradaDePrueba entrada = new EntradaDePrueba("prueba");

        assertThrows(IllegalArgumentException.class,
                () -> entrada.validarPublico(2.0, 0.0, 1.0));
    }

    @Test
    void desconexionActivaSoloLaEntradaAfectada() {
        AtomicInteger notificaciones = new AtomicInteger();
        EntradaDePrueba afectada = new EntradaDePrueba(
                "afectada", notificaciones);
        EntradaDePrueba operativa = new EntradaDePrueba("operativa");

        afectada.iniciar();
        operativa.iniciar();
        afectada.desconectar("simulada");

        assertFalse(afectada.estaActiva());
        assertTrue(operativa.estaActiva());
        assertEquals(1, notificaciones.get());

        operativa.detener();
    }

    @Test
    void excluyeTecladoYMouseYValidaPosicionDeTableta() {
        assertFalse(EntradaControl.esDispositivoValido("USB Keyboard"));
        assertFalse(EntradaControl.esDispositivoValido("Mouse"));
        assertTrue(EntradaControl.esDispositivoValido("Gamepad"));

        EntradaTableta tableta = new EntradaTableta();
        assertThrows(IllegalArgumentException.class,
                () -> tableta.normalizarPosicion(
                        Double.NEGATIVE_INFINITY, 0.0, 0.5));
    }

    private static final class EntradaDePrueba extends Entrada {

        EntradaDePrueba(String nombre) {
            super(nombre);
        }

        EntradaDePrueba(String nombre,
                        AtomicInteger notificaciones) {
            super(nombre, entrada -> notificaciones.incrementAndGet());
        }

        void validarPublico(double valor, double minimo, double maximo) {
            validarValor("valor", valor, minimo, maximo);
        }

        @Override
        protected void ejecutarCaptura() throws Exception {
            while (estaActiva()) {
                Thread.sleep(5);
            }
        }
    }
}
