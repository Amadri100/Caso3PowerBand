package powerband.main;

import javax.swing.SwingUtilities;

import powerband.controlador.FachadaControlador;
import powerband.controlador.FachadaControladorFalsa;
import powerband.ui.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(crearFachadaDeDesarrollo());
            ventana.setVisible(true);
        });
    }

    private static FachadaControlador crearFachadaDeDesarrollo() {
        return new FachadaControladorFalsa();
    }
}
