package powerband.ui;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;

public final class VentanaPrincipal extends JFrame {

    private final FachadaControlador controlador;
    private final Runnable manejadorCierreCompletado;
    private Runnable accionCierre = this::iniciarCierre;
    private boolean cierreEnCurso;
    private boolean cierreCompletado;

    public VentanaPrincipal(FachadaControlador controlador) {
        this(controlador, () -> { });
    }

    protected VentanaPrincipal(
            FachadaControlador controlador,
            Runnable manejadorCierreCompletado) {
        super("PowerBand");
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");
        this.manejadorCierreCompletado = Objects.requireNonNull(
                manejadorCierreCompletado, "manejadorCierreCompletado");

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

    public final void cerrar() {
        iniciarCierre();
    }

    public final void limpiarObservadorEstado() {
        PanelPrincipal panel =
                (PanelPrincipal) getContentPane();
        panel.limpiarEstado();
    }

    protected boolean cierreCompletado() {
        return cierreCompletado;
    }

    private void iniciarCierre() {
        if (cierreEnCurso) {
            return;
        }
        cierreEnCurso = true;
        limpiarObservadorEstado();
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                controlador.desconectar();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    registrarFallo(excepcion);
                } catch (ExecutionException excepcion) {
                    Throwable causa = excepcion.getCause();
                    registrarFallo(causa == null ? excepcion : causa);
                } finally {
                    boolean anterior = cierreCompletado;
                    cierreCompletado = true;
                    firePropertyChange(
                            "cierreCompletado",
                            anterior,
                            cierreCompletado);
                    manejadorCierreCompletado.run();
                    dispose();
                }
            }
        }.execute();
    }

    private static void registrarFallo(Throwable fallo) {
        LoggerPowerBand.getInstance().registrarError(
                "Fallo al cerrar PowerBand mediante el controlador: "
                        + fallo);
    }
}
