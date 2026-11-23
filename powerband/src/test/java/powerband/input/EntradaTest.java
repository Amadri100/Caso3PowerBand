package powerband.input;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
    void iniciaYDetieneCadaEntradaIndependientemente() throws Exception {
        EntradaDePrueba primera = new EntradaDePrueba("primera", null);
        EntradaDePrueba segunda = new EntradaDePrueba("segunda", null);

        primera.iniciar();
        segunda.iniciar();
        try {
            assertTrue(primera.esperarInicio());
            assertTrue(segunda.esperarInicio());
            primera.detener();

            assertFalse(primera.estaActiva());
            assertTrue(segunda.estaActiva());
            assertEquals(1, primera.recursosLiberados.get());
            assertTrue(primera.esperarFinalizacion());
        } finally {
            primera.detener();
            segunda.detener();
        }
        assertFalse(segunda.estaActiva());
        assertEquals(1, segunda.recursosLiberados.get());
    }

    @Test
    void rechazaValoresFueraDeRango() {
        EntradaDePrueba entrada = new EntradaDePrueba("prueba");

        assertThrows(IllegalArgumentException.class,
                () -> entrada.validarPublico(2.0, 0.0, 1.0));
    }

    @Test
    void desconexionActivaSoloLaEntradaAfectada() throws Exception {
        AtomicInteger notificaciones = new AtomicInteger();
        EntradaDePrueba afectada = new EntradaDePrueba(
                "afectada", notificaciones, null);
        EntradaDePrueba operativa = new EntradaDePrueba("operativa", null);

        afectada.iniciar();
        operativa.iniciar();
        try {
            assertTrue(afectada.esperarInicio());
            assertTrue(operativa.esperarInicio());
            afectada.desconectar("simulada");

            assertFalse(afectada.estaActiva());
            assertTrue(operativa.estaActiva());
            assertEquals(1, notificaciones.get());
            assertEquals(1, afectada.recursosLiberados.get());
            assertTrue(Files.readString(
                    carpetaTemporal.resolve("powerband.log"))
                    .contains("Desconexion de entrada afectada: simulada"));
        } finally {
            afectada.detener();
            operativa.detener();
        }
    }

    @Test
    void conservaLaCausaOriginalAlPropagarUnaDesconexion()
            throws Exception {
        RuntimeException causaOriginal =
                new IllegalStateException("fallo de captura");
        AtomicReference<Throwable> causaNotificada =
                new AtomicReference<>();
        CountDownLatch notificacion = new CountDownLatch(1);
        EntradaConFallo entrada = new EntradaConFallo(causaOriginal);
        entrada.registrarObservadorDesconexion(
                (origen, motivo, causa) -> {
                    causaNotificada.set(causa);
                    notificacion.countDown();
                });

        entrada.iniciar();

        assertTrue(notificacion.await(1, TimeUnit.SECONDS));
        assertSame(causaOriginal, causaNotificada.get());
    }

    @Test
    void entradaMicrofonoRechazaYRegistraFrecuenciaFueraDeRango()
            throws Exception {
        EntradaMicrofono microfono = new EntradaMicrofono();

        assertThrows(IllegalArgumentException.class,
                () -> microfono.procesarFrecuencia(10.0, 0.5));

        String log = Files.readString(
                carpetaTemporal.resolve("powerband.log"));
        assertTrue(log.contains("[error]"));
        assertTrue(log.contains("frecuencia=10.0"));
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

        private final CountDownLatch inicio;
        private final CountDownLatch finalizacion = new CountDownLatch(1);
        private final AtomicInteger recursosLiberados = new AtomicInteger();

        EntradaDePrueba(String nombre) {
            super(nombre);
            inicio = new CountDownLatch(0);
        }

        EntradaDePrueba(String nombre,
                        AtomicInteger notificaciones,
                        CountDownLatch inicio) {
            super(nombre, entrada -> notificaciones.incrementAndGet());
            this.inicio = inicio == null
                    ? new CountDownLatch(1)
                    : inicio;
        }

        EntradaDePrueba(String nombre, CountDownLatch inicio) {
            super(nombre);
            this.inicio = inicio == null
                    ? new CountDownLatch(1)
                    : inicio;
        }

        boolean esperarInicio() throws InterruptedException {
            return inicio.await(1, TimeUnit.SECONDS);
        }

        boolean esperarFinalizacion() throws InterruptedException {
            return finalizacion.await(1, TimeUnit.SECONDS);
        }

        void validarPublico(double valor, double minimo, double maximo) {
            validarValor("valor", valor, minimo, maximo);
        }

        @Override
        protected void ejecutarCaptura() throws Exception {
            inicio.countDown();
            try {
                while (estaActiva()) {
                    Thread.sleep(50);
                }
            } finally {
                finalizacion.countDown();
            }
        }

        @Override
        protected void liberarRecursosEspecificos() {
            recursosLiberados.incrementAndGet();
        }
    }

    private static final class EntradaConFallo extends Entrada {

        private final RuntimeException fallo;

        private EntradaConFallo(RuntimeException fallo) {
            super("con-fallo");
            this.fallo = fallo;
        }

        @Override
        protected void ejecutarCaptura() {
            throw fallo;
        }
    }
}
