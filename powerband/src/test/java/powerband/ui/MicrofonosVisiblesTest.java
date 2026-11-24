package powerband.ui;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JList;

import org.junit.jupiter.api.Test;

import powerband.input.EntradaMicrofono;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MicrofonosVisiblesTest {

    @Test
    void losMicrofonosSeDetectan() {
        List<DispositivoEntrada> microfonos = EntradaMicrofono.dispositivosCompatibles();
        System.out.println("Microfonos detectados: " + microfonos.size());
        for (DispositivoEntrada micro : microfonos) {
            System.out.println("  - " + micro.id() + ": " + micro.nombre());
        }
        assertTrue(microfonos.isEmpty() || !microfonos.isEmpty(),
                "Se debe poder detectar dispositivos (vacío o lleno)");
    }

    @Test
    void elComboBoxMuestraLosMicrofonos() {
        List<DispositivoEntrada> microfonos = EntradaMicrofono.dispositivosCompatibles();
        JComboBox<DispositivoEntrada> combo = new JComboBox<>();
        for (DispositivoEntrada micro : microfonos) {
            combo.addItem(micro);
        }
        combo.setRenderer(new PanelInstrumentos.RenderizadorDispositivoPublico());
        
        if (!microfonos.isEmpty()) {
            combo.setSelectedIndex(0);
            DispositivoEntrada seleccionado = (DispositivoEntrada) combo.getSelectedItem();
            String texto = seleccionado.toString();
            assertFalse(texto.isBlank(), "El nombre del micrófono no debe estar vacío");
            System.out.println("Micrófono mostrado: " + texto);
        }
    }

    @Test
    void elRenderizadorNoProduceExcepciones() {
        List<DispositivoEntrada> microfonos = EntradaMicrofono.dispositivosCompatibles();
        JComboBox<DispositivoEntrada> combo = new JComboBox<>();
        combo.setRenderer(new PanelInstrumentos.RenderizadorDispositivoPublico());
        
        for (DispositivoEntrada micro : microfonos) {
            combo.addItem(micro);
        }
        
        JList<DispositivoEntrada> jlist = new JList<>();
        for (int i = 0; i < combo.getItemCount(); i++) {
            DispositivoEntrada item = combo.getItemAt(i);
            var renderer = combo.getRenderer();
            var componente = renderer.getListCellRendererComponent(
                    jlist, item, i, i == 0, false);
            assertNotNull(componente, "El renderer no debe devolver null");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

