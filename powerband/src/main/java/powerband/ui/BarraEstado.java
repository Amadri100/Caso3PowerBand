package powerband.ui;

import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import powerband.controlador.EstadoControlador;
import powerband.controlador.FachadaControlador;
import powerband.controlador.ObservadorEstado;

public final class BarraEstado extends JPanel
        implements ObservadorEstado {

    private final FachadaControlador controlador;
    private final JLabel etiquetaEstado = new JLabel(
            "Estado: esperando notificación",
            SwingConstants.LEFT);
    private final JLabel etiquetaError = new JLabel(
            "Sin errores",
            SwingConstants.LEFT);
    private boolean registrada;

    public BarraEstado(FachadaControlador controlador) {
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");
        setLayout(new FlowLayout(
                FlowLayout.LEFT,
                ConstantesGUI.ESPACIADO_TARJETAS,
                ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_VERTICAL));
        setBackground(ConstantesGUI.COLOR_BARRA);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        1, 0, 0, 0, ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_INTERIOR_BARRA_ESTADO,
                        ConstantesGUI.MARGEN_AREA_HORIZONTAL,
                        ConstantesGUI.MARGEN_INTERIOR_BARRA_ESTADO,
                        ConstantesGUI.MARGEN_AREA_HORIZONTAL)));

        etiquetaEstado.setForeground(ConstantesGUI.COLOR_BLANCO);
        etiquetaError.setForeground(
                ConstantesGUI.COLOR_TEXTO_MENU_INACTIVO);
        add(etiquetaEstado);
        add(etiquetaError);

        registrar();
    }

    public final void limpiar() {
        if (!registrada) {
            return;
        }
        registrada = false;
        controlador.retirarObservadorEstado(this);
    }

    @Override
    public final void estadoCambiado(EstadoControlador estado) {
        Objects.requireNonNull(estado, "estado");
        SwingUtilities.invokeLater(() ->
                etiquetaEstado.setText("Estado: " + textoEstado(estado)));
    }

    @Override
    public final void errorNotificado(String mensaje, Throwable causa) {
        String texto = mensaje == null || mensaje.isBlank()
                ? "Error: ocurrió un fallo"
                : "Error: " + mensaje;
        SwingUtilities.invokeLater(() -> etiquetaError.setText(texto));
    }

    protected JLabel etiquetaEstado() {
        return etiquetaEstado;
    }

    protected JLabel etiquetaError() {
        return etiquetaError;
    }

    protected boolean registrada() {
        return registrada;
    }

    private void registrar() {
        if (registrada) {
            return;
        }
        controlador.registrarObservadorEstado(this);
        registrada = true;
    }

    private static String textoEstado(EstadoControlador estado) {
        return switch (estado) {
            case INICIAL -> "Inicial";
            case CONECTANDO -> "Conectando";
            case CONECTADO -> "Conectado";
            case GRABANDO -> "Grabando";
            case REPRODUCIENDO -> "Reproduciendo";
            case ERROR -> "Error";
        };
    }

    @Override
    public void dispositivoDesconectado(String instrumentoId) {
 
    }
}
