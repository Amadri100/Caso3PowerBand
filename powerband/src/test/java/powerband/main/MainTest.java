package powerband.main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.controlador.Controlador;
import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;
import powerband.util.Configuraciones;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void registraExcepcionesNoCapturadasYDelegaAlManejadorAnterior(@TempDir Path carpeta)
            throws Exception {
        Thread.UncaughtExceptionHandler anterior = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> recibido = new AtomicReference<>();
        LoggerPowerBand.cambiarPath(carpeta);
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, true);
        try {
            Thread.setDefaultUncaughtExceptionHandler((hilo, fallo) -> recibido.set(fallo));
            Main.instalarManejadorExcepciones();
            RuntimeException fallo = new RuntimeException("fallo sin capturar");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), fallo);
            assertSame(fallo, recibido.get());
            String linea = Files.readString(carpeta.resolve("powerband.log"));
            assertTrue(linea.contains("origen=MAIN"));
            assertTrue(linea.contains("fallo sin capturar"));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(anterior);
            Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, false);
            LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
        }
    }

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
