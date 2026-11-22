package powerband.ui;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.ObservadorEstado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VentanaPrincipalTest {

    @Test
    public void cerrarDelegaUnaDesconexionYRetiraElObservadorUnaVez()
            throws Exception {
        AtomicInteger desconexiones = new AtomicInteger();
        AtomicInteger retiros = new AtomicInteger();
        AtomicReference<ObservadorEstado> observador =
                new AtomicReference<>();
        CountDownLatch desconexionIniciada = new CountDownLatch(1);
        CountDownLatch permitirDesconexion = new CountDownLatch(1);
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("registrarObservadorEstado")) {
                observador.set((ObservadorEstado) argumentos[0]);
                return null;
            }
            if (metodo.getName().equals("retirarObservadorEstado")) {
                assertSame(observador.get(), argumentos[0]);
                retiros.incrementAndGet();
                return null;
            }
            if (metodo.getName().equals("desconectar")) {
                assertFalse(SwingUtilities.isEventDispatchThread());
                desconexiones.incrementAndGet();
                desconexionIniciada.countDown();
                esperar(permitirDesconexion);
                return null;
            }
            if (metodo.getName().equals("listarSesiones")
                    || metodo.getName().equals("listarInstrumentos")
                    || metodo.getName().equals("listarDispositivos")
                    || metodo.getName().equals("listarSintetizadores")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")
                    || metodo.getName().equals(
                            "dispositivoPorInstrumento")
                    || metodo.getName().equals(
                            "sintetizadoresPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        });
        AtomicReference<VentanaPrincipal> ventanaReferencia =
                new AtomicReference<>();
        CountDownLatch cierreCompletado = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(
                            fachada, cierreCompletado::countDown);
            ventanaReferencia.set(ventana);
            ventana.cerrar();
            ventana.cerrar();
        });

        assertTrue(desconexionIniciada.await(5, TimeUnit.SECONDS));
        permitirDesconexion.countDown();
        assertTrue(cierreCompletado.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() ->
                assertTrue(ventanaReferencia.get()
                        .cierreCompletado()));

        assertEquals(1, desconexiones.get());
        assertEquals(1, retiros.get());
    }

    @Test
    public void fallaDeDesconexionSeRegistraYLaVentanaIgualSeDispone()
            throws Exception {
        AtomicInteger desconexiones = new AtomicInteger();
        CountDownLatch desconexionIniciada = new CountDownLatch(1);
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("desconectar")) {
                desconexiones.incrementAndGet();
                desconexionIniciada.countDown();
                throw new IllegalStateException("fallo al desconectar");
            }
            if (metodo.getName().equals("listarSesiones")
                    || metodo.getName().equals("listarInstrumentos")
                    || metodo.getName().equals("listarDispositivos")
                    || metodo.getName().equals("listarSintetizadores")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")
                    || metodo.getName().equals(
                            "dispositivoPorInstrumento")
                    || metodo.getName().equals(
                            "sintetizadoresPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        });
        AtomicReference<VentanaPrincipal> ventanaReferencia =
                new AtomicReference<>();
        CountDownLatch cierreCompletado = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(
                            fachada, cierreCompletado::countDown);
            ventanaReferencia.set(ventana);
            ventana.cerrar();
        });

        assertTrue(desconexionIniciada.await(5, TimeUnit.SECONDS));
        assertTrue(cierreCompletado.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() ->
                assertTrue(ventanaReferencia.get()
                        .cierreCompletado()));
        assertEquals(1, desconexiones.get());
    }

    @Test
    public void accionDeCierrePersonalizadaPuedeUsarseEnPruebas()
            throws Exception {
        CountDownLatch accionEjecutada = new CountDownLatch(1);
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")
                    || metodo.getName().equals("listarInstrumentos")
                    || metodo.getName().equals("listarDispositivos")
                    || metodo.getName().equals("listarSintetizadores")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")
                    || metodo.getName().equals(
                            "dispositivoPorInstrumento")
                    || metodo.getName().equals(
                            "sintetizadoresPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        });
        AtomicReference<VentanaPrincipal> ventanaReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(fachada);
            ventanaReferencia.set(ventana);
            ventana.establecerAccionCierre(
                    accionEjecutada::countDown);
        });
        SwingUtilities.invokeLater(() ->
                ventanaReferencia.get().dispatchEvent(
                        new java.awt.event.WindowEvent(
                                ventanaReferencia.get(),
                                java.awt.event.WindowEvent.WINDOW_CLOSING)));

        assertTrue(accionEjecutada.await(5, TimeUnit.SECONDS));
    }

    private static FachadaControlador fachada(
            BiFunction<Method, Object[], Object> manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) ->
                        manejador.apply(metodo, argumentos));
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Tiempo de espera agotado");
            }
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new AssertionError(excepcion);
        }
    }

}
