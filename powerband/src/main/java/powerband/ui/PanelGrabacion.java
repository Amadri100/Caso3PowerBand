package powerband.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import powerband.controlador.FachadaControlador;
import powerband.excepciones.NombreDuplicadoException;
import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;

public final class PanelGrabacion extends JPanel {

    private final FachadaControlador controlador;
    private final Supplier<String> solicitadorNombre;
    private final Consumer<String> avisadorDuplicado;
    private final Consumer<Throwable> manejadorErrores;
    private final JButton botonIniciar = new JButton("Iniciar grabación");
    private final JButton botonDetener = new JButton("Detener grabación");
    private boolean operacionEnCurso;

    public PanelGrabacion(FachadaControlador controlador) {
        this(
                controlador,
                PanelGrabacion::solicitarNombre,
                PanelGrabacion::mostrarAvisoDuplicado,
                PanelGrabacion::registrarError);
    }

    protected PanelGrabacion(
            FachadaControlador controlador,
            Supplier<String> solicitadorNombre,
            Consumer<String> avisadorDuplicado,
            Consumer<Throwable> manejadorErrores) {
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");
        this.solicitadorNombre = Objects.requireNonNull(
                solicitadorNombre, "solicitadorNombre");
        this.avisadorDuplicado = Objects.requireNonNull(
                avisadorDuplicado, "avisadorDuplicado");
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
                "Graba los comandos enviados y asigna un nombre al finalizar.");
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

        botonIniciar.setForeground(ConstantesGUI.COLOR_BLANCO);
        botonIniciar.setBackground(ConstantesGUI.COLOR_ACENTO);
        botonIniciar.setFocusPainted(false);
        botonIniciar.addActionListener(
                evento -> iniciarGrabacion());
        acciones.add(botonIniciar);

        botonDetener.setFocusPainted(false);
        botonDetener.addActionListener(
                evento -> solicitarDetencion());
        acciones.add(botonDetener);

        add(acciones, BorderLayout.CENTER);
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected JButton botonIniciar() {
        return botonIniciar;
    }

    protected JButton botonDetener() {
        return botonDetener;
    }

    private void iniciarGrabacion() {
        ejecutarOperacion(() -> {
            controlador.iniciarGrabacion();
            return Boolean.TRUE;
        }, resultado -> { }, manejadorErrores);
    }

    private void solicitarDetencion() {
        if (operacionEnCurso) {
            return;
        }

        String nombre;
        try {
            nombre = solicitadorNombre.get();
        } catch (RuntimeException fallo) {
            manejadorErrores.accept(fallo);
            return;
        }
        if (nombre == null) {
            return;
        }

        ejecutarOperacion(() -> {
            List<String> coincidencias = Objects.requireNonNull(
                    controlador.buscarSesiones(nombre),
                    "buscarSesiones");
            if (!coincidencias.isEmpty()) {
                return Boolean.FALSE;
            }
            controlador.detenerGrabacion(nombre);
            return Boolean.TRUE;
        }, finalizada -> {
            if (!finalizada) {
                LoggerPowerBand.getInstance().registrar(Log.error(
                        "Nombre duplicado de grabación: " + nombre, OrigenLog.UI, null));
                avisadorDuplicado.accept(nombre);
                solicitarDetencion();
            }
        }, fallo -> {
            if (fallo instanceof NombreDuplicadoException) {
                LoggerPowerBand.getInstance().registrar(Log.error(
                        "Nombre duplicado de grabación: " + nombre, OrigenLog.UI, fallo));
                avisadorDuplicado.accept(nombre);
            } else {
                manejadorErrores.accept(fallo);
            }
        });
    }

    private <T> void ejecutarOperacion(
            Callable<T> operacion,
            Consumer<T> alCompletar,
            Consumer<Throwable> alFallar) {
        if (operacionEnCurso) {
            return;
        }

        operacionEnCurso = true;
        actualizarDisponibilidad(false);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                T resultado = null;
                Throwable fallo = null;
                try {
                    resultado = get();
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    fallo = excepcion;
                } catch (ExecutionException excepcion) {
                    fallo = excepcion.getCause() == null
                            ? excepcion
                            : excepcion.getCause();
                } finally {
                    operacionEnCurso = false;
                    actualizarDisponibilidad(true);
                }

                if (fallo == null) {
                    alCompletar.accept(resultado);
                } else {
                    alFallar.accept(fallo);
                }
            }
        }.execute();
    }

    private void actualizarDisponibilidad(boolean habilitados) {
        botonIniciar.setEnabled(habilitados);
        botonDetener.setEnabled(habilitados);
    }

    private static String solicitarNombre() {
        return JOptionPane.showInputDialog(
                null,
                "Nombre de la grabación:",
                "Finalizar grabación",
                JOptionPane.QUESTION_MESSAGE);
    }

    private static void mostrarAvisoDuplicado(String nombre) {
        JOptionPane.showMessageDialog(
                null,
                "Ya existe una grabación con el nombre \"" + nombre
                        + "\". Elige otro nombre.",
                "Nombre duplicado",
                JOptionPane.WARNING_MESSAGE);
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrar(Log.error(
                "Fallo en una operación del panel de grabación: " + fallo,
                OrigenLog.UI, fallo));
    }
}
