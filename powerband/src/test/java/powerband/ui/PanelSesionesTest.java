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

import javax.swing.AbstractButton;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PanelSesionesTest {

    @Test
    public void cargaLasSesionesEnElOrdenDeLaFachadaFueraDelEdt()
            throws Exception {
        List<String> sesiones = List.of("Primera", "Segunda");
        AtomicInteger consultas = new AtomicInteger();
        CountDownLatch consultaIniciada = new CountDownLatch(1);
        CountDownLatch permitirConsulta = new CountDownLatch(1);
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")) {
                consultas.incrementAndGet();
                assertFalse(SwingUtilities.isEventDispatchThread());
                consultaIniciada.countDown();
                esperar(permitirConsulta);
                return sesiones;
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelSesiones> panelReferencia =
                new AtomicReference<>();
        CountDownLatch botonHabilitado = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            PanelSesiones panel = new PanelSesiones(controlador);
            panelReferencia.set(panel);
            panel.botonActualizar().addPropertyChangeListener(
                    "enabled", evento -> {
                        if (Boolean.TRUE.equals(evento.getNewValue())) {
                            botonHabilitado.countDown();
                        }
                    });
        });

        assertTrue(consultaIniciada.await(5, TimeUnit.SECONDS));
        permitirConsulta.countDown();
        assertTrue(botonHabilitado.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            JList<String> lista = panelReferencia.get().listaSesiones();
            assertEquals(2, lista.getModel().getSize());
            assertEquals("Primera", lista.getModel().getElementAt(0));
            assertEquals("Segunda", lista.getModel().getElementAt(1));
            assertFalse(panelReferencia.get().botonReproducir().isEnabled());
        });
        assertEquals(1, consultas.get());
    }

    @Test
    public void delegaLaBusquedaYUnaBusquedaVaciaRecargaLaLista()
            throws Exception {
        AtomicInteger consultas = new AtomicInteger();
        AtomicInteger busquedas = new AtomicInteger();
        AtomicReference<String> nombreBuscado = new AtomicReference<>();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")) {
                consultas.incrementAndGet();
                return List.of("Completa");
            }
            if (metodo.getName().equals("buscarSesiones")) {
                busquedas.incrementAndGet();
                nombreBuscado.set((String) argumentos[0]);
                return List.of("Coincidencia");
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelSesiones> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelSesiones(controlador)));
        esperarDisponible(panelReferencia.get().botonActualizar());

        SwingUtilities.invokeAndWait(() -> {
            PanelSesiones panel = panelReferencia.get();
            panel.campoBusqueda().setText("demo");
            panel.botonBuscar().doClick();
        });
        esperarDisponible(panelReferencia.get().botonActualizar());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Coincidencia",
                    panelReferencia.get().listaSesiones()
                            .getModel().getElementAt(0));
            panelReferencia.get().campoBusqueda().setText("  ");
            panelReferencia.get().botonBuscar().doClick();
        });
        esperarDisponible(panelReferencia.get().botonActualizar());

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Completa",
                    panelReferencia.get().listaSesiones()
                            .getModel().getElementAt(0));
        });
        assertEquals(1, busquedas.get());
        assertEquals("demo", nombreBuscado.get());
        assertEquals(2, consultas.get());
    }

    @Test
    public void muestraEstadosVaciosParaListaYBusquedaSinCoincidencias()
            throws Exception {
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")
                    || metodo.getName().equals("buscarSesiones")) {
                return List.of();
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelSesiones> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelSesiones(controlador)));
        esperarDisponible(panelReferencia.get().botonActualizar());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(
                    "No hay sesiones guardadas.",
                    panelReferencia.get().mensajeEstado().getText());
            panelReferencia.get().campoBusqueda().setText("no existe");
            panelReferencia.get().botonBuscar().doClick();
        });
        esperarDisponible(panelReferencia.get().botonActualizar());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(
                    "No se encontraron sesiones.",
                    panelReferencia.get().mensajeEstado().getText());
            assertEquals(0, panelReferencia.get().listaSesiones()
                    .getModel().getSize());
        });
    }

    @Test
    public void reproduceSoloLaSesionSeleccionadaYSinConfirmarSiDevuelveFalse()
            throws Exception {
        AtomicReference<String> sesionReproducida = new AtomicReference<>();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")) {
                return List.of("Primera", "Segunda");
            }
            if (metodo.getName().equals("reproducirSesion")) {
                sesionReproducida.set((String) argumentos[0]);
                return false;
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelSesiones> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelSesiones(controlador)));
        esperarDisponible(panelReferencia.get().botonActualizar());
        SwingUtilities.invokeAndWait(() -> {
            PanelSesiones panel = panelReferencia.get();
            assertFalse(panel.botonReproducir().isEnabled());
            panel.listaSesiones().setSelectedIndex(1);
            assertTrue(panel.botonReproducir().isEnabled());
            panel.botonReproducir().doClick();
        });
        esperarDisponible(panelReferencia.get().botonActualizar());
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(
                    "No se pudo iniciar la reproducción.",
                    panelReferencia.get().mensajeEstado().getText());
        });
        assertEquals("Segunda", sesionReproducida.get());
    }

    @Test
    public void conservaLaListaSiLaConsultaFalla()
            throws Exception {
        AtomicInteger consultas = new AtomicInteger();
        AtomicReference<Throwable> errorCapturado = new AtomicReference<>();
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")) {
                if (consultas.getAndIncrement() == 0) {
                    return List.of("Conservada");
                }
                throw new IllegalStateException("fallo de consulta");
            }
            throw new AssertionError(
                    "Operación inesperada: " + metodo.getName());
        });
        AtomicReference<PanelSesiones> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> panelReferencia.set(
                new PanelSesiones(controlador, errorCapturado::set)));
        esperarDisponible(panelReferencia.get().botonActualizar());

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.get().botonActualizar().doClick());
        esperarDisponible(panelReferencia.get().botonActualizar());

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, panelReferencia.get().listaSesiones()
                    .getModel().getSize());
            assertEquals("Conservada", panelReferencia.get().listaSesiones()
                    .getModel().getElementAt(0));
            assertEquals(
                    "No se pudieron cargar las sesiones.",
                    panelReferencia.get().mensajeEstado().getText());
        });
        assertNotNull(errorCapturado.get());
        assertEquals("fallo de consulta",
                errorCapturado.get().getMessage());
    }

    @Test
    public void panelPrincipalIntegraLaVistaDeSesiones()
            throws Exception {
        FachadaControlador controlador = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarSesiones")) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(metodo.getReturnType())) {
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
        AtomicReference<PanelPrincipal> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelPrincipal(controlador)));

        SwingUtilities.invokeAndWait(() -> {
            assertInstanceOf(
                    PanelSesiones.class,
                    panelReferencia.get().panelVista(Vista.SESIONES));
        });
    }

    private static FachadaControlador fachada(
            BiFunction<Method, Object[], Object> manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) ->
                        manejador.apply(metodo, argumentos));
    }

    private static void esperarDisponible(AbstractButton boton)
            throws Exception {
        CountDownLatch disponible = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            if (boton.isEnabled()) {
                disponible.countDown();
            } else {
                boton.addPropertyChangeListener(
                        "enabled", evento -> {
                            if (Boolean.TRUE.equals(evento.getNewValue())) {
                                disponible.countDown();
                            }
                        });
            }
        });
        assertTrue(disponible.await(5, TimeUnit.SECONDS));
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
