package powerband.ui;

import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelConexionTest {

    @Test
    void conservaLaFachadaYNoConectaAlConstruirse() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador controlador = fachadaEspia(metodo -> {
            llamadas.incrementAndGet();
            return respuestaPorDefecto(metodo.getReturnType());
        });
        AtomicReference<PanelConexion> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelConexion panel = new PanelConexion(
                    controlador, fallo -> { });
            panelReferencia.set(panel);
            assertSame(controlador, panel.fachada());
            assertTrue(panel.botonConectar().isEnabled());
            assertTrue(panel.botonDesconectar().isEnabled());
        });

        assertEquals(0, llamadas.get());
    }

    @Test
    void conectarSeEjecutaFueraDelEdtYBloqueaAmbosBotonesHastaTerminar()
            throws Exception {
        CountDownLatch operacionIniciada = new CountDownLatch(1);
        CountDownLatch permitirTerminar = new CountDownLatch(1);
        CountDownLatch botonesHabilitados = new CountDownLatch(1);
        AtomicInteger conexiones = new AtomicInteger();
        AtomicInteger desconexiones = new AtomicInteger();
        FachadaControlador controlador = fachadaEspia(metodo -> {
            if (metodo.getName().equals("conectar")) {
                conexiones.incrementAndGet();
                assertFalse(SwingUtilities.isEventDispatchThread());
                operacionIniciada.countDown();
                try {
                    assertTrue(permitirTerminar.await(
                            5, TimeUnit.SECONDS));
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(excepcion);
                }
                return false;
            }
            if (metodo.getName().equals("desconectar")) {
                desconexiones.incrementAndGet();
                return null;
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelConexion> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelConexion panel = new PanelConexion(
                    controlador, fallo -> { });
            panelReferencia.set(panel);
            panel.botonConectar().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())) {
                            botonesHabilitados.countDown();
                        }
                    });
            panel.botonConectar().doClick();
            assertFalse(panel.botonConectar().isEnabled());
            assertFalse(panel.botonDesconectar().isEnabled());
        });

        assertTrue(operacionIniciada.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            PanelConexion panel = panelReferencia.get();
            panel.botonDesconectar().doClick();
            assertFalse(panel.botonConectar().isEnabled());
            assertFalse(panel.botonDesconectar().isEnabled());
        });
        assertEquals(0, desconexiones.get());

        permitirTerminar.countDown();
        assertTrue(botonesHabilitados.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            PanelConexion panel = panelReferencia.get();
            assertTrue(panel.botonConectar().isEnabled());
            assertTrue(panel.botonDesconectar().isEnabled());
            assertEquals("Conectar", panel.botonConectar().getText());
            assertEquals("Desconectar", panel.botonDesconectar().getText());
        });
        assertEquals(1, conexiones.get());
        assertEquals(0, desconexiones.get());
    }

    @Test
    void delegaDesconexionYEnviaExcepcionesAlManejador()
            throws Exception {
        CountDownLatch errorRecibido = new CountDownLatch(1);
        CountDownLatch botonHabilitado = new CountDownLatch(1);
        AtomicInteger desconexiones = new AtomicInteger();
        AtomicReference<Throwable> error = new AtomicReference<>();
        FachadaControlador controlador = fachadaEspia(metodo -> {
            if (metodo.getName().equals("desconectar")) {
                desconexiones.incrementAndGet();
                throw new IllegalStateException("fallo de desconexión");
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });

        SwingUtilities.invokeAndWait(() -> {
            PanelConexion panel = new PanelConexion(controlador, fallo -> {
                error.set(fallo);
                errorRecibido.countDown();
            });
            panel.botonDesconectar().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())) {
                            botonHabilitado.countDown();
                        }
                    });
            panel.botonDesconectar().doClick();
            assertFalse(panel.botonDesconectar().isEnabled());
        });

        assertTrue(errorRecibido.await(5, TimeUnit.SECONDS));
        assertTrue(botonHabilitado.await(5, TimeUnit.SECONDS));
        assertEquals(1, desconexiones.get());
        assertTrue(error.get() instanceof IllegalStateException);
        assertEquals("fallo de desconexión", error.get().getMessage());
    }

    private static FachadaControlador fachadaEspia(
            java.util.function.Function<java.lang.reflect.Method, Object>
                    manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) -> manejador.apply(metodo));
    }

    private static Object respuestaPorDefecto(Class<?> tipoRetorno) {
        if (tipoRetorno == boolean.class) {
            return false;
        }
        return null;
    }
}
