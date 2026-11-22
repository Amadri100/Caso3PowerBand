package powerband.ui;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PanelPrincipalTest {

    @Test
    public void conservaLaFachadaYConsultaLosPanelesAlConstruirse()
            throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador fachada = fachadaContada(llamadas);
        AtomicReference<PanelPrincipal> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelPrincipal panel = new PanelPrincipal(fachada);
            panelReferencia.set(panel);
            assertSame(fachada, panel.fachada());
            assertEquals(Vista.INICIO, panel.vistaActual());
            assertInstanceOf(
                    PanelGrabacion.class,
                    panel.panelVista(Vista.GRABACION));
            assertInstanceOf(
                    PanelSesiones.class,
                    panel.panelVista(Vista.SESIONES));
        });

        esperarDisponible(
                ((PanelSesiones) panelReferencia.get()
                        .panelVista(Vista.SESIONES)).botonActualizar());
        assertEquals(3, llamadas.get());
    }

    @Test
    public void navegarMuestraUnaVistaYNoDuplicaLosPaneles()
            throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador fachada = fachadaContada(llamadas);
        AtomicReference<PanelPrincipal> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelPrincipal panel = new PanelPrincipal(fachada);
            panelReferencia.set(panel);
        });

        PanelPrincipal panel = panelReferencia.get();
        esperarDisponible(
                ((PanelSesiones) panel.panelVista(Vista.SESIONES))
                        .botonActualizar());
        int llamadasIniciales = llamadas.get();
        SwingUtilities.invokeAndWait(() -> {
            int cantidadPaneles = panel.cantidadVistas();
            for (Vista vista : Vista.values()) {
                panel.botonNavegacion(vista).doClick();
                assertEquals(vista, panel.vistaActual());
                assertTrue(panel.panelVista(vista).isVisible());
                assertEquals(1, cantidadPanelesVisibles(panel));
                assertEquals(cantidadPaneles, panel.cantidadVistas());
            }
        });
        assertEquals(llamadasIniciales, llamadas.get());
    }

    private static int cantidadPanelesVisibles(PanelPrincipal panel) {
        int visibles = 0;
        for (Vista vista
                : Vista.values()) {
            JPanel panelVista = panel.panelVista(vista);
            if (panelVista.isVisible()) {
                visibles++;
            }
        }
        return visibles;
    }

    private static FachadaControlador fachadaContada(
            AtomicInteger llamadas) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) -> {
                    llamadas.incrementAndGet();
                    Class<?> tipoRetorno = metodo.getReturnType();
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
                });
    }

    private static void esperarDisponible(JButton boton)
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
}
