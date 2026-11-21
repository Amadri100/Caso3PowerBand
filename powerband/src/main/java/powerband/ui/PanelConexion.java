package powerband.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;

public final class PanelConexion extends JPanel {

    private final FachadaControlador controlador;
    private final Consumer<Throwable> manejadorErrores;
    private final JButton botonConectar = new JButton("Conectar");
    private final JButton botonDesconectar = new JButton("Desconectar");
    private boolean operacionEnCurso;

    public PanelConexion(FachadaControlador controlador) {
        this(controlador, PanelConexion::registrarError);
    }

    PanelConexion(FachadaControlador controlador,
            Consumer<Throwable> manejadorErrores) {
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");
        this.manejadorErrores = Objects.requireNonNull(
                manejadorErrores, "manejadorErrores");

        setLayout(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_INICIO));
        setBackground(ConstantesGUI.COLOR_FONDO);
        setBorder(BorderFactory.createEmptyBorder(
                ConstantesGUI.MARGEN_MARCADOR_VERTICAL,
                ConstantesGUI.MARGEN_MARCADOR_HORIZONTAL,
                ConstantesGUI.MARGEN_MARCADOR_VERTICAL,
                ConstantesGUI.MARGEN_MARCADOR_HORIZONTAL));

        JLabel instrucciones = new JLabel(
                "Inicia o finaliza la conexión mediante el controlador.");
        instrucciones.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
        add(instrucciones, BorderLayout.NORTH);

        JPanel acciones = new JPanel(new FlowLayout(
                FlowLayout.LEFT, ConstantesGUI.ESPACIADO_TARJETAS, 0));
        acciones.setBackground(ConstantesGUI.COLOR_BLANCO);
        acciones.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL)));

        botonConectar.setForeground(ConstantesGUI.COLOR_BLANCO);
        botonConectar.setBackground(ConstantesGUI.COLOR_ACENTO);
        botonConectar.setFocusPainted(false);
        botonConectar.addActionListener(
                evento -> ejecutarOperacion(controlador::conectar));
        acciones.add(botonConectar);

        botonDesconectar.setHorizontalAlignment(SwingConstants.CENTER);
        botonDesconectar.setFocusPainted(false);
        botonDesconectar.addActionListener(
                evento -> ejecutarOperacion(() -> {
                    controlador.desconectar();
                    return Boolean.TRUE;
                }));
        acciones.add(botonDesconectar);

        add(acciones, BorderLayout.CENTER);
    }

    JButton botonConectar() {
        return botonConectar;
    }

    JButton botonDesconectar() {
        return botonDesconectar;
    }

    FachadaControlador fachada() {
        return controlador;
    }

    private void ejecutarOperacion(
            Callable<Boolean> operacion) {
        if (operacionEnCurso) {
            return;
        }

        operacionEnCurso = true;
        actualizarDisponibilidad(false);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    manejadorErrores.accept(excepcion);
                } catch (ExecutionException excepcion) {
                    Throwable causa = excepcion.getCause();
                    manejadorErrores.accept(
                            causa == null ? excepcion : causa);
                } finally {
                    operacionEnCurso = false;
                    actualizarDisponibilidad(true);
                }
            }
        }.execute();
    }

    private void actualizarDisponibilidad(boolean habilitados) {
        botonConectar.setEnabled(habilitados);
        botonDesconectar.setEnabled(habilitados);
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrarError(
                "Fallo en una operación del panel de conexión: " + fallo);
    }
}
