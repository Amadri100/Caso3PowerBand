package powerband.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;

public final class PanelInstrumentos extends JPanel {

    private final FachadaControlador controlador;
    private final Consumer<Throwable> manejadorErrores;
    private final DefaultListModel<String> modeloInstrumentos =
            new DefaultListModel<>();
    private final JList<String> listaInstrumentos =
            new JList<>(modeloInstrumentos);
    private final JComboBox<String> selectorDispositivo =
            new JComboBox<>();
    private final DefaultListModel<IdSintetizador> modeloSintetizadores =
            new DefaultListModel<>();
    private final JList<IdSintetizador> listaSintetizadores =
            new JList<>(modeloSintetizadores);
    private final JButton botonDispositivo =
            new JButton("Asignar dispositivo");
    private final JButton botonSintetizadores =
            new JButton("Asignar sintetizadores");
    private final JLabel mensajeEstado =
            new JLabel("Cargando instrumentos...", SwingConstants.LEFT);
    private boolean operacionEnCurso;

    public PanelInstrumentos(FachadaControlador controlador) {
        this(controlador, PanelInstrumentos::registrarError);
    }

    protected PanelInstrumentos(
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

        add(crearPanelInstrumentos(), BorderLayout.WEST);
        add(crearPanelAsignacion(), BorderLayout.CENTER);

        listaInstrumentos.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        listaSintetizadores.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        listaInstrumentos.addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                cargarSeleccion();
            }
        });
        botonDispositivo.addActionListener(
                evento -> asignarDispositivo());
        botonSintetizadores.addActionListener(
                evento -> asignarSintetizadores());

        actualizarCatalogo();
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected JList<String> listaInstrumentos() {
        return listaInstrumentos;
    }

    protected JComboBox<String> selectorDispositivo() {
        return selectorDispositivo;
    }

    protected JList<IdSintetizador> listaSintetizadores() {
        return listaSintetizadores;
    }

    protected JButton botonDispositivo() {
        return botonDispositivo;
    }

    protected JButton botonSintetizadores() {
        return botonSintetizadores;
    }

    protected JLabel mensajeEstado() {
        return mensajeEstado;
    }

    private JPanel crearPanelInstrumentos() {
        JPanel panel = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_VERTICAL));
        panel.setBackground(ConstantesGUI.COLOR_BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL)));
        panel.add(mensajeEstado, BorderLayout.NORTH);
        panel.add(new JScrollPane(listaInstrumentos), BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearPanelAsignacion() {
        JPanel panel = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_INICIO));
        panel.setBackground(ConstantesGUI.COLOR_FONDO);

        JPanel dispositivo = new JPanel(new FlowLayout(
                FlowLayout.LEFT, ConstantesGUI.ESPACIADO_TARJETAS, 0));
        dispositivo.setBackground(ConstantesGUI.COLOR_BLANCO);
        dispositivo.add(new JLabel("Dispositivo:"));
        dispositivo.add(selectorDispositivo);
        dispositivo.add(botonDispositivo);
        panel.add(dispositivo, BorderLayout.NORTH);

        JPanel sintetizadores = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_TARJETAS));
        sintetizadores.setBackground(ConstantesGUI.COLOR_BLANCO);
        sintetizadores.setBorder(BorderFactory.createEmptyBorder(
                ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                ConstantesGUI.MARGEN_TARJETA_HORIZONTAL));
        sintetizadores.add(
                new JLabel("Sintetizadores asignados:",
                        SwingConstants.LEFT),
                BorderLayout.NORTH);
        sintetizadores.add(
                new JScrollPane(listaSintetizadores),
                BorderLayout.CENTER);
        sintetizadores.add(botonSintetizadores, BorderLayout.SOUTH);
        panel.add(sintetizadores, BorderLayout.CENTER);

        return panel;
    }

    private void actualizarCatalogo() {
        ejecutarOperacion(() -> new Catalogo(
                Objects.requireNonNull(
                        controlador.listarInstrumentos(),
                        "listarInstrumentos"),
                Objects.requireNonNull(
                        controlador.listarDispositivos(),
                        "listarDispositivos"),
                Objects.requireNonNull(
                        controlador.listarSintetizadores(),
                        "listarSintetizadores"),
                Objects.requireNonNull(
                        controlador.dispositivoPorInstrumento(),
                        "dispositivoPorInstrumento"),
                Objects.requireNonNull(
                        controlador.sintetizadoresPorInstrumento(),
                        "sintetizadoresPorInstrumento")),
                catalogo -> {
                    modeloInstrumentos.clear();
                    for (String instrumento : catalogo.instrumentos()) {
                        modeloInstrumentos.addElement(instrumento);
                    }
                    selectorDispositivo.removeAllItems();
                    selectorDispositivo.addItem("Sin dispositivo");
                    for (String dispositivo : catalogo.dispositivos()) {
                        selectorDispositivo.addItem(dispositivo);
                    }
                    modeloSintetizadores.clear();
                    for (IdSintetizador sintetizador
                            : catalogo.sintetizadores()) {
                        modeloSintetizadores.addElement(sintetizador);
                    }
                    if (!modeloInstrumentos.isEmpty()) {
                        listaInstrumentos.setSelectedIndex(0);
                        cargarSeleccion(
                                catalogo.dispositivosPorInstrumento(),
                                catalogo.sintetizadoresPorInstrumento());
                    } else {
                        mensajeEstado.setText(
                                "No hay instrumentos disponibles.");
                    }
                },
                fallo -> {
                    mensajeEstado.setText(
                            "No se pudo cargar la asignación.");
                    manejadorErrores.accept(fallo);
                });
    }

    private void cargarSeleccion() {
        ejecutarOperacion(() -> new Catalogo(
                Objects.requireNonNull(
                        controlador.listarInstrumentos(),
                        "listarInstrumentos"),
                Objects.requireNonNull(
                        controlador.listarDispositivos(),
                        "listarDispositivos"),
                Objects.requireNonNull(
                        controlador.listarSintetizadores(),
                        "listarSintetizadores"),
                Objects.requireNonNull(
                        controlador.dispositivoPorInstrumento(),
                        "dispositivoPorInstrumento"),
                Objects.requireNonNull(
                        controlador.sintetizadoresPorInstrumento(),
                        "sintetizadoresPorInstrumento")),
                catalogo -> cargarSeleccion(
                        catalogo.dispositivosPorInstrumento(),
                        catalogo.sintetizadoresPorInstrumento()),
                fallo -> manejadorErrores.accept(fallo));
    }

    private void cargarSeleccion(
            Map<String, String> dispositivos,
            Map<String, List<IdSintetizador>> sintetizadores) {
        String instrumento = listaInstrumentos.getSelectedValue();
        if (instrumento == null) {
            actualizarDisponibilidad(false);
            return;
        }

        String dispositivo = dispositivos.get(instrumento);
        if (dispositivo == null) {
            selectorDispositivo.setSelectedItem("Sin dispositivo");
        } else {
            selectorDispositivo.setSelectedItem(dispositivo);
        }

        List<IdSintetizador> asignados =
                sintetizadores.getOrDefault(instrumento, List.of());
        listaSintetizadores.clearSelection();
        for (int indice = 0;
                indice < modeloSintetizadores.size();
                indice++) {
            if (asignados.contains(
                    modeloSintetizadores.getElementAt(indice))) {
                listaSintetizadores.addSelectionInterval(indice, indice);
            }
        }
        mensajeEstado.setText(dispositivo == null
                ? "Instrumento: " + instrumento + " (sin dispositivo)"
                : "Instrumento: " + instrumento
                        + " (dispositivo activo)");
        actualizarDisponibilidad(!operacionEnCurso);
    }

    private void asignarDispositivo() {
        String instrumento = listaInstrumentos.getSelectedValue();
        Object seleccionado = selectorDispositivo.getSelectedItem();
        if (instrumento == null || seleccionado == null
                || "Sin dispositivo".equals(seleccionado)) {
            return;
        }

        String dispositivo = (String) seleccionado;
        ejecutarOperacion(
                () -> controlador.asignarDispositivo(
                        instrumento, dispositivo),
                asignado -> {
                    if (asignado) {
                        mensajeEstado.setText(
                                "Instrumento activo: " + instrumento);
                    } else {
                        mensajeEstado.setText(
                                "No se pudo asignar el dispositivo.");
                    }
                },
                fallo -> {
                    mensajeEstado.setText(
                            "No se pudo asignar el dispositivo.");
                    manejadorErrores.accept(fallo);
                });
    }

    private void asignarSintetizadores() {
        String instrumento = listaInstrumentos.getSelectedValue();
        if (instrumento == null) {
            return;
        }

        Set<IdSintetizador> seleccionados = new LinkedHashSet<>(
                listaSintetizadores.getSelectedValuesList());
        List<IdSintetizador> ids = new ArrayList<>(seleccionados);
        ejecutarOperacion(
                () -> controlador.reemplazarSintetizadores(
                        instrumento, ids),
                asignado -> {
                    if (asignado) {
                        mensajeEstado.setText(
                                "Sintetizadores actualizados.");
                    } else {
                        mensajeEstado.setText(
                                "No se pudo actualizar los sintetizadores.");
                    }
                },
                fallo -> {
                    mensajeEstado.setText(
                            "No se pudo actualizar los sintetizadores.");
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
                    actualizarDisponibilidad(
                            listaInstrumentos.getSelectedIndex() >= 0);
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
        boolean disponible = habilitados && !operacionEnCurso;
        listaInstrumentos.setEnabled(disponible);
        selectorDispositivo.setEnabled(disponible);
        listaSintetizadores.setEnabled(disponible);
        botonDispositivo.setEnabled(disponible);
        botonSintetizadores.setEnabled(disponible);
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrarError(
                "Fallo en una operación del panel de instrumentos: "
                        + fallo);
    }

    private record Catalogo(
            List<String> instrumentos,
            List<String> dispositivos,
            List<IdSintetizador> sintetizadores,
            Map<String, String> dispositivosPorInstrumento,
            Map<String, List<IdSintetizador>>
                    sintetizadoresPorInstrumento) {
    }
}
