package powerband.ui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import powerband.controlador.FachadaControlador;

public final class VentanaPrincipal extends JFrame {

    private final FachadaControlador controlador;
    private Runnable accionCierre = this::dispose;

    public VentanaPrincipal(FachadaControlador controlador) {
        super("PowerBand");
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(
                ConstantesGUI.ANCHO_MINIMO_VENTANA,
                ConstantesGUI.ALTO_MINIMO_VENTANA));
        setSize(ConstantesGUI.ANCHO_INICIAL_VENTANA,
                ConstantesGUI.ALTO_INICIAL_VENTANA);
        setLocationByPlatform(true);
        setContentPane(new PanelPrincipal(controlador));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                accionCierre.run();
            }
        });
    }

    public FachadaControlador controlador() {
        return controlador;
    }

    public void establecerAccionCierre(Runnable accionCierre) {
        this.accionCierre = Objects.requireNonNull(
                accionCierre, "accionCierre");
    }

    public final void limpiarObservadorEstado() {
        PanelPrincipal panel =
                (PanelPrincipal) getContentPane();
        panel.limpiarEstado();
    }
}
