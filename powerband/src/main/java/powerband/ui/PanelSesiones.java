package powerband.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;

public final class PanelSesiones extends JPanel {

    private final FachadaControlador controlador;
    private final Consumer<Throwable> manejadorErrores;
    private final DefaultListModel<String> modeloSesiones =
            new DefaultListModel<>();
    private final JList<String> listaSesiones =
            new JList<>(modeloSesiones);
    private final JTextField campoBusqueda =
            new JTextField(ConstantesGUI.COLUMNAS_CAMPO_CONFIGURACION);
    private final JButton botonBuscar = new JButton("Buscar");
    private final JButton botonMostrarTodas = new JButton("Mostrar todas");
    private final JButton botonActualizar = new JButton("Actualizar");
    private final JButton botonReproducir = new JButton("Reproducir");
    private final JLabel mensajeEstado = new JLabel(
            "Cargando sesiones...", SwingConstants.LEFT);
    private boolean operacionEnCurso;

    public PanelSesiones(FachadaControlador controlador) {
        this(controlador, PanelSesiones::registrarError);
    }

    protected PanelSesiones(
            FachadaControlador controlador,
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

        add(crearControlesBusqueda(), BorderLayout.NORTH);
        add(crearAreaLista(), BorderLayout.CENTER);
        add(crearAcciones(), BorderLayout.SOUTH);

        listaSesiones.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        listaSesiones.addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                actualizarDisponibilidad();
            }
        });
        botonBuscar.addActionListener(evento -> buscar());
        campoBusqueda.addActionListener(evento -> buscar());
        botonMostrarTodas.addActionListener(
                evento -> mostrarTodasLasSesiones());
        botonActualizar.addActionListener(
                evento -> actualizarLista());
        botonReproducir.addActionListener(
                evento -> reproducirSeleccion());

        cargarSesiones(
                controlador::listarSesiones,
                "No hay sesiones guardadas.");
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected JList<String> listaSesiones() {
        return listaSesiones;
    }

    protected JTextField campoBusqueda() {
        return campoBusqueda;
    }

    protected JButton botonBuscar() {
        return botonBuscar;
    }

    protected JButton botonMostrarTodas() {
        return botonMostrarTodas;
    }

    protected JButton botonActualizar() {
        return botonActualizar;
    }

    protected JButton botonReproducir() {
        return botonReproducir;
    }

    protected JLabel mensajeEstado() {
        return mensajeEstado;
    }

    private JPanel crearControlesBusqueda() {
        JPanel controles = new JPanel(new FlowLayout(
                FlowLayout.LEFT, ConstantesGUI.ESPACIADO_TARJETAS, 0));
        controles.setBackground(ConstantesGUI.COLOR_FONDO);
        controles.add(new JLabel("Buscar sesión:"));
        controles.add(campoBusqueda);
        controles.add(botonBuscar);
        controles.add(botonMostrarTodas);
        controles.add(botonActualizar);
        return controles;
    }

    private JPanel crearAreaLista() {
        JPanel area = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_VERTICAL));
        area.setBackground(ConstantesGUI.COLOR_BLANCO);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL)));
        mensajeEstado.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
        area.add(mensajeEstado, BorderLayout.NORTH);
        area.add(new JScrollPane(listaSesiones), BorderLayout.CENTER);
        return area;
    }

    private JPanel crearAcciones() {
        JPanel acciones = new JPanel(new FlowLayout(
                FlowLayout.RIGHT, ConstantesGUI.ESPACIADO_TARJETAS, 0));
        acciones.setBackground(ConstantesGUI.COLOR_FONDO);
        botonReproducir.setForeground(ConstantesGUI.COLOR_BLANCO);
        botonReproducir.setBackground(ConstantesGUI.COLOR_ACENTO);
        botonReproducir.setFocusPainted(false);
        botonReproducir.setEnabled(false);
        acciones.add(botonReproducir);
        return acciones;
    }

    private void buscar() {
        String nombre = campoBusqueda.getText();
        if (nombre.isBlank()) {
            actualizarLista();
            return;
        }
        cargarSesiones(
                () -> controlador.buscarSesiones(nombre),
                "No se encontraron sesiones.");
    }

    private void mostrarTodasLasSesiones() {
        campoBusqueda.setText("");
        actualizarLista();
    }

    private void actualizarLista() {
        campoBusqueda.setText("");
        cargarSesiones(
                controlador::listarSesiones,
                "No hay sesiones guardadas.");
    }

    private void cargarSesiones(
            Callable<List<String>> consulta,
            String mensajeListaVacia) {
        mensajeEstado.setText("Cargando sesiones...");
        ejecutarOperacion(() -> {
            List<String> sesiones = Objects.requireNonNull(
                    consulta.call(), "resultado de sesiones");
            for (String sesion : sesiones) {
                Objects.requireNonNull(sesion, "nombre de sesión");
            }
            return sesiones;
        }, sesiones -> {
            modeloSesiones.clear();
            for (String sesion : sesiones) {
                modeloSesiones.addElement(sesion);
            }
            mensajeEstado.setText(
                    sesiones.isEmpty()
                            ? mensajeListaVacia
                            : "Sesiones disponibles: " + sesiones.size());
        }, fallo -> {
            mensajeEstado.setText("No se pudieron cargar las sesiones.");
            manejadorErrores.accept(fallo);
        });
    }

    private void reproducirSeleccion() {
        String nombre = listaSesiones.getSelectedValue();
        if (nombre == null || operacionEnCurso) {
            return;
        }

        ejecutarOperacion(
                () -> controlador.reproducirSesion(nombre),
                iniciada -> {
                    if (!iniciada) {
                        mensajeEstado.setText(
                                "No se pudo iniciar la reproducción.");
                    }
                },
                fallo -> {
                    mensajeEstado.setText(
                            "No se pudo solicitar la reproducción.");
                    manejadorErrores.accept(fallo);
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
        actualizarDisponibilidad();
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
                    actualizarDisponibilidad();
                }

                if (fallo == null) {
                    alCompletar.accept(resultado);
                } else {
                    alFallar.accept(fallo);
                }
            }
        }.execute();
    }

    private void actualizarDisponibilidad() {
        boolean habilitados = !operacionEnCurso;
        campoBusqueda.setEnabled(habilitados);
        botonBuscar.setEnabled(habilitados);
        botonMostrarTodas.setEnabled(habilitados);
        botonActualizar.setEnabled(habilitados);
        listaSesiones.setEnabled(habilitados);
        botonReproducir.setEnabled(
                habilitados && listaSesiones.getSelectedIndex() >= 0);
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrarError(
                "Fallo en una operación del panel de sesiones: " + fallo);
    }
}
