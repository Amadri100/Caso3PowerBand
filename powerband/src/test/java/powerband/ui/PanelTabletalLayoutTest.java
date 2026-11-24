package powerband.ui;

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.FachadaControladorFalsa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PanelTabletalLayoutTest {

    @Test
    void elPanelTablestaMuestraBotonActualizarDispositivos() throws InterruptedException {
        FachadaControlador controlador = new FachadaControladorFalsa();
        PanelInstrumentos panel = new PanelInstrumentos(controlador);

        JFrame frame = new JFrame("Test Panel Layout");
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);

        try {
            Thread.sleep(500);

            panel.listaInstrumentos().setSelectedValue("tableta", true);
            Thread.sleep(1000);

            var panelTableta = buscarPanelTableta(panel);
            assertNotNull(panelTableta, "El panel de tableta debe estar presente");
            assertTrue(panelTableta.isVisible(), "El panel de tableta debe ser visible");

            var botonActualizar = buscarBotonActualizar(panel);
            assertNotNull(botonActualizar, "El botón actualizar debe estar presente");
            assertTrue(botonActualizar.isVisible(), "El botón actualizar debe ser visible");
            assertTrue(botonActualizar.isEnabled(), "El botón actualizar debe estar habilitado");

            System.out.println("✓ Panel de tableta visible");
            System.out.println("✓ Botón 'Actualizar dispositivos' visible y habilitado");
            System.out.println("  Tamaño frame: " + frame.getSize());
            System.out.println("  Tamaño panel: " + panel.getSize());

        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    private JPanel buscarPanelTableta(Component componente) {
        if (componente instanceof JPanel) {
            JPanel panel = (JPanel) componente;
            if (panel.getComponentCount() > 0) {
                for (Component child : panel.getComponents()) {
                    if (child instanceof JPanel && child.isVisible()) {
                        JPanel subpanel = (JPanel) child;
                        if (subpanel.toString().contains("JPanel")) {
                            for (Component subchild : subpanel.getComponents()) {
                                if (subchild instanceof JPanel) {
                                    return (JPanel) subchild;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private javax.swing.JButton buscarBotonActualizar(Component componente) {
        if (componente instanceof javax.swing.JButton) {
            javax.swing.JButton btn = (javax.swing.JButton) componente;
            if ("Actualizar dispositivos".equals(btn.getText())) {
                return btn;
            }
        }
        if (componente instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) componente).getComponents()) {
                javax.swing.JButton resultado = buscarBotonActualizar(child);
                if (resultado != null) {
                    return resultado;
                }
            }
        }
        return null;
    }
}
