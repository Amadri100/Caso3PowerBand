package powerband.ui;

import java.util.List;

import javax.swing.JFrame;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.FachadaControladorFalsa;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PanelInstrumentosMicrofonosTest {

    @Test
    void elSelectorDeMicrofonosMuestraDispositivosCompatibles() {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador, error -> {
            System.err.println("Error: " + error);
        });

        List<DispositivoEntrada> microfonos = controlador.listarDispositivos("microfono");
        System.out.println("Microfonos disponibles: " + microfonos.size());
        for (DispositivoEntrada micro : microfonos) {
            System.out.println("  - " + micro.id() + ": " + micro.nombre());
        }

        assertNotNull(microfonos, "La lista de microfonos no debe ser null");
        assertFalse(microfonos.isEmpty(), "Debe haber al menos un microfono disponible");
    }

    @Test
    void elComboBoxSeInicializaConRenderer() throws InterruptedException {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador);

        JFrame frame = new JFrame("Test Panel");
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        try {
            Thread.sleep(500);
            var selectorDispositivo = panel.selectorDispositivo();
            assertNotNull(selectorDispositivo.getRenderer(),
                    "El selector debe tener un renderer configurado");
            System.out.println("Renderer configurado: " + selectorDispositivo.getRenderer().getClass().getSimpleName());
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    @Test
    void elPanelMuestraLosMicrofonosAlSeleccionarMicrofono() throws InterruptedException {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador);

        JFrame frame = new JFrame("Test Panel");
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        try {
            Thread.sleep(500);

            panel.listaInstrumentos().setSelectedValue("microfono", true);
            Thread.sleep(1000);

            var selector = panel.selectorDispositivo();
            int itemCount = selector.getItemCount();
            System.out.println("Items en el selector después de seleccionar microfono: " + itemCount);

            if (itemCount > 0) {
                for (int i = 0; i < itemCount; i++) {
                    DispositivoEntrada item = selector.getItemAt(i);
                    System.out.println("  [" + i + "] " + item.toString());
                }
            }

            assertFalse(itemCount == 0, "El selector debe mostrar dispositivos");
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}

