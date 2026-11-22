package powerband.ui;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.EstadoControlador;
import powerband.controlador.FachadaControlador;
import powerband.controlador.ObservadorEstado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BarraEstadoTest {

    @Test
    public void registraUnObservadorYLaNavegacionNoLoDuplica()
            throws Exception {
        AtomicInteger registros = new AtomicInteger();
        AtomicReference<ObservadorEstado> observador =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("registrarObservadorEstado")) {
                registros.incrementAndGet();
                observador.set((ObservadorEstado) argumentos[0]);
                return null;
            }
            if (metodo.getName().equals("listarSesiones")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            return null;
        });
        AtomicReference<PanelPrincipal> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelPrincipal(fachada)));

        assertEquals(1, registros.get());
        assertNotNull(observador.get());
        SwingUtilities.invokeAndWait(() -> {
            for (Vista vista : Vista.values()) {
                panelReferencia.get().botonNavegacion(vista).doClick();
            }
        });
        assertEquals(1, registros.get());
    }

    @Test
    public void presentaEstadosYErroresRecibidosFueraDelEdt()
            throws Exception {
        AtomicReference<ObservadorEstado> observador =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("registrarObservadorEstado")) {
                observador.set((ObservadorEstado) argumentos[0]);
                return null;
            }
            if (metodo.getName().equals("listarSesiones")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            return null;
        });
        AtomicReference<BarraEstado> barraReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                barraReferencia.set(
                        new BarraEstado(fachada)));

        CountDownLatch estadoActualizado = new CountDownLatch(1);
        SwingUtilities.invokeLater(estadoActualizado::countDown);
        assertTrue(estadoActualizado.await(5, TimeUnit.SECONDS));

        CountDownLatch estados = new CountDownLatch(
                EstadoControlador.values().length);
        SwingUtilities.invokeAndWait(() -> {
            barraReferencia.get().etiquetaEstado()
                    .addPropertyChangeListener("text", evento ->
                            estados.countDown());
        });

        for (EstadoControlador estado : EstadoControlador.values()) {
            assertFalse(SwingUtilities.isEventDispatchThread());
            observador.get().estadoCambiado(estado);
        }
        assertTrue(estados.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() ->
                assertEquals(
                        "Estado: Error",
                        barraReferencia.get().etiquetaEstado().getText()));

        CountDownLatch errorActualizado = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            barraReferencia.get().etiquetaError()
                    .addPropertyChangeListener("text", evento ->
                            errorActualizado.countDown());
        });
        observador.get().errorNotificado(
                "No se pudo conectar", new IllegalStateException());
        assertTrue(errorActualizado.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(
                    "Error: No se pudo conectar",
                    barraReferencia.get().etiquetaError().getText());
            observador.get().estadoCambiado(
                    EstadoControlador.CONECTADO);
        });

        CountDownLatch estadoConectado = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            assertEquals(
                    "Estado: Conectado",
                    barraReferencia.get().etiquetaEstado().getText());
            assertEquals(
                    "Error: No se pudo conectar",
                    barraReferencia.get().etiquetaError().getText());
            estadoConectado.countDown();
        });
        assertTrue(estadoConectado.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void limpiezaRetiraElObservadorUnaSolaVez()
            throws Exception {
        AtomicInteger retiros = new AtomicInteger();
        AtomicReference<ObservadorEstado> observador =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("registrarObservadorEstado")) {
                observador.set((ObservadorEstado) argumentos[0]);
                return null;
            }
            if (metodo.getName().equals("retirarObservadorEstado")) {
                assertEquals(
                        observador.get(), argumentos[0]);
                retiros.incrementAndGet();
                return null;
            }
            if (metodo.getName().equals("listarSesiones")) {
                return List.of();
            }
            if (metodo.getName().equals("opcionesConfigurables")) {
                return Map.of();
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        });
        AtomicReference<BarraEstado> barraReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                barraReferencia.set(new BarraEstado(fachada)));

        SwingUtilities.invokeAndWait(() -> {
            barraReferencia.get().limpiar();
            barraReferencia.get().limpiar();
        });
        assertEquals(1, retiros.get());
        assertFalse(barraReferencia.get().registrada());
    }

    private static FachadaControlador fachada(
            BiFunction<Method, Object[], Object> manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) ->
                        manejador.apply(metodo, argumentos));
    }
}
