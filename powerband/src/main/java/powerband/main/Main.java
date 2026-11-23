package powerband.main;

import javax.swing.SwingUtilities;

import powerband.controlador.Controlador;
import powerband.controlador.FachadaControlador;
import powerband.ui.VentanaPrincipal;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(crearFachada());
            ventana.setVisible(true);
        });
    }

    static FachadaControlador crearFachada() {
        return Controlador.getInstance();
    }
}
