package powerband.ui;

import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.FachadaControladorFalsa;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PanelInstrumentosTableraIntegracionTest {

    @Test
    void alSeleccionarTableraSeDetectanDispositivosCorrectamente() throws InterruptedException {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador);

        JFrame frame = new JFrame("Test Panel Integration");
        frame.add(panel);
        frame.pack();
        frame.setSize(1300, 900);
        frame.setVisible(true);

        try {
            Thread.sleep(500);

            System.out.println("Seleccionando tableta...");
            panel.listaInstrumentos().setSelectedValue("tableta", true);
            
            Thread.sleep(500);
            System.out.println("Esperando a que se detecten dispositivos...");
            Thread.sleep(2000);

            List<DispositivoEntrada> tabletas = controlador.listarDispositivos("tableta");
            assertNotNull(tabletas, "La lista de tabletas no debe ser null");

            System.out.println("✓ Tabletas detectadas: " + tabletas.size());
            for (DispositivoEntrada tableta : tabletas) {
                System.out.println("  - " + tableta.id() + ": " + tableta.nombre());
            }

            System.out.println("✓ Panel de tableta visible: " + panel.isVisible());
            var selector = panel.selectorDispositivo();
            System.out.println("✓ Selector tiene " + selector.getItemCount() + " items");

        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    @Test
    void alSeleccionarMicrofonoSeDetectanDispositivosCorrectamente() throws InterruptedException {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador);

        JFrame frame = new JFrame("Test Panel Integration");
        frame.add(panel);
        frame.pack();
        frame.setSize(1300, 900);
        frame.setVisible(true);

        try {
            Thread.sleep(500);

            System.out.println("Seleccionando micrófono...");
            panel.listaInstrumentos().setSelectedValue("microfono", true);
            
            Thread.sleep(1000);

            List<DispositivoEntrada> microfonos = controlador.listarDispositivos("microfono");
            assertNotNull(microfonos, "La lista de microfonos no debe ser null");
            assertFalse(microfonos.isEmpty(), "Debe haber al menos un micrófono disponible");

            System.out.println("✓ Microfonos detectados: " + microfonos.size());
            for (DispositivoEntrada micro : microfonos) {
                System.out.println("  - " + micro.id() + ": " + micro.nombre());
            }

            var selector = panel.selectorDispositivo();
            System.out.println("✓ Selector tiene " + selector.getItemCount() + " items");
            if (selector.getItemCount() > 0) {
                DispositivoEntrada item = selector.getItemAt(0);
                System.out.println("  Primer item mostrado: " + item.toString());
            }

        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}
