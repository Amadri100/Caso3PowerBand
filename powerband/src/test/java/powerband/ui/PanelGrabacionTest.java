package powerband.ui;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelGrabacionTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @AfterEach
    void restaurarLogger() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void conservaLaFachadaYNoIniciaNiDetieneAlConstruirse()
            throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            llamadas.incrementAndGet();
            return respuestaPorDefecto(metodo.getReturnType());
        });

        SwingUtilities.invokeAndWait(() -> {
            PanelGrabacion panel = new PanelGrabacion(
                    controlador, () -> null, nombre -> { }, fallo -> { });
            assertSame(controlador, panel.fachada());
            assertTrue(panel.botonIniciar().isEnabled());
            assertTrue(panel.botonDetener().isEnabled());
        });

        assertEquals(0, llamadas.get());
    }

    @Test
    void iniciarSeEjecutaFueraDelEdtYBloqueaLosBotonesHastaTerminar()
            throws Exception {
        CountDownLatch operacionIniciada = new CountDownLatch(1);
        CountDownLatch permitirTerminar = new CountDownLatch(1);
        CountDownLatch botonesHabilitados = new CountDownLatch(1);
        AtomicInteger inicios = new AtomicInteger();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("iniciarGrabacion")) {
                inicios.incrementAndGet();
                assertFalse(SwingUtilities.isEventDispatchThread());
                operacionIniciada.countDown();
                try {
                    assertTrue(permitirTerminar.await(
                            5, TimeUnit.SECONDS));
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(excepcion);
                }
                return null;
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelGrabacion> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelGrabacion panel = new PanelGrabacion(
                    controlador, () -> null, nombre -> { }, fallo -> { });
            panelReferencia.set(panel);
            panel.botonIniciar().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())) {
                            botonesHabilitados.countDown();
                        }
                    });
            panel.botonIniciar().doClick();
            assertFalse(panel.botonIniciar().isEnabled());
            assertFalse(panel.botonDetener().isEnabled());
        });

        assertTrue(operacionIniciada.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            panelReferencia.get().botonIniciar().doClick();
            assertEquals(1, inicios.get());
        });

        permitirTerminar.countDown();
        assertTrue(botonesHabilitados.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(panelReferencia.get().botonIniciar().isEnabled());
            assertTrue(panelReferencia.get().botonDetener().isEnabled());
        });
        assertEquals(1, inicios.get());
    }

    @Test
    void cancelacionDelDialogoNoConsultaNiFinalizaLaGrabacion()
            throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            llamadas.incrementAndGet();
            return respuestaPorDefecto(metodo.getReturnType());
        });

        SwingUtilities.invokeAndWait(() -> {
            PanelGrabacion panel = new PanelGrabacion(
                    controlador, () -> null, nombre -> { }, fallo -> { });
            panel.botonDetener().doClick();
        });

        assertEquals(0, llamadas.get());
    }

    @Test
    void advierteDuplicadosYPermiteReintentarConOtroNombre()
            throws Exception {
        AtomicInteger solicitudesNombre = new AtomicInteger();
        AtomicInteger avisos = new AtomicInteger();
        AtomicReference<String> nombreFinalizado = new AtomicReference<>();
        CountDownLatch botonesHabilitadosDespuesDeGuardar =
                new CountDownLatch(1);
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("buscarSesiones")) {
                String nombre = (String) argumentos[0];
                return nombre.equals("Existe")
                        ? List.of(nombre)
                        : List.of();
            }
            if (metodo.getName().equals("detenerGrabacion")) {
                nombreFinalizado.set(
                        (String) argumentos[0]);
                return null;
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelGrabacion> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelGrabacion panel = new PanelGrabacion(
                    controlador,
                    () -> solicitudesNombre.getAndIncrement() == 0
                            ? "Existe"
                            : "Nueva",
                    nombre -> avisos.incrementAndGet(),
                    fallo -> {
                        throw new AssertionError(fallo);
                    });
            panelReferencia.set(panel);
            panel.botonDetener().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())
                                && solicitudesNombre.get() == 2) {
                            botonesHabilitadosDespuesDeGuardar.countDown();
                        }
                    });
            panel.botonDetener().doClick();
        });

        assertTrue(botonesHabilitadosDespuesDeGuardar.await(
                5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(panelReferencia.get().botonDetener().isEnabled());
            assertEquals(2, solicitudesNombre.get());
            assertEquals(1, avisos.get());
        });
        assertEquals("Nueva", nombreFinalizado.get());
        String log = Files.readString(carpetaTemporal.resolve("powerband.log"));
        assertTrue(log.contains("Nombre duplicado de grabación: Existe"));
    }

    @Test
    void enviaFallosDeFinalizacionAlManejadorDeErrores()
            throws Exception {
        CountDownLatch falloRecibido = new CountDownLatch(1);
        CountDownLatch botonHabilitado = new CountDownLatch(1);
        AtomicReference<Throwable> falloCapturado = new AtomicReference<>();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("buscarSesiones")) {
                return List.of();
            }
            if (metodo.getName().equals("detenerGrabacion")) {
                throw new IllegalStateException("fallo al detener");
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });

        SwingUtilities.invokeAndWait(() -> {
            PanelGrabacion panel = new PanelGrabacion(
                    controlador,
                    () -> "Nueva",
                    nombre -> {
                        throw new AssertionError("No era un duplicado");
                    },
                    fallo -> {
                        falloCapturado.set(fallo);
                        falloRecibido.countDown();
                    });
            panel.botonDetener().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())) {
                            botonHabilitado.countDown();
                        }
                    });
            panel.botonDetener().doClick();
            assertFalse(panel.botonDetener().isEnabled());
        });

        assertTrue(falloRecibido.await(5, TimeUnit.SECONDS));
        assertTrue(botonHabilitado.await(5, TimeUnit.SECONDS));
        assertEquals("fallo al detener",
                falloCapturado.get().getMessage());
    }

    private static FachadaControlador fachada(
            BiFunction<java.lang.reflect.Method, Object[], Object> manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) ->
                        manejador.apply(metodo, argumentos));
    }

    private static Object respuestaPorDefecto(Class<?> tipoRetorno) {
        if (tipoRetorno == boolean.class) {
            return false;
        }
        if (List.class.isAssignableFrom(tipoRetorno)) {
            return List.of();
        }
        if (Map.class.isAssignableFrom(tipoRetorno)) {
            return Map.of();
        }
        return null;
    }
}
