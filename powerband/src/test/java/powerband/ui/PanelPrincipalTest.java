package powerband.ui;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelPrincipalTest {

    @Test
    void conservaLaFachadaInyectadaSinInvocarla() throws Exception {
        AtomicInteger llamadas = new AtomicInteger();
        FachadaControlador fachada = fachadaContada(llamadas);

        SwingUtilities.invokeAndWait(() -> {
            PanelPrincipal panel = new PanelPrincipal(fachada);
            assertSame(fachada, panel.fachada());
            assertEquals(Vista.INICIO, panel.vistaActual());
        });

        assertEquals(0, llamadas.get());
    }

    @Test
    void navegarMuestraUnaVistaYNoDuplicaLosPaneles() throws Exception {
        FachadaControlador fachada = fachadaContada(new AtomicInteger());

        SwingUtilities.invokeAndWait(() -> {
            PanelPrincipal panel = new PanelPrincipal(fachada);
            int cantidadPaneles = panel.cantidadVistas();

            for (Vista vista
                    : Vista.values()) {
                panel.botonNavegacion(vista).doClick();

                assertEquals(vista, panel.vistaActual());
                assertTrue(panel.panelVista(vista).isVisible());
                assertEquals(1, cantidadPanelesVisibles(panel));
                assertEquals(cantidadPaneles, panel.cantidadVistas());
            }
        });
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
}
