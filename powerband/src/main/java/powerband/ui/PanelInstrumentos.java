package powerband.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import powerband.controlador.EstadoControlador;
import powerband.controlador.FachadaControlador;
import powerband.controlador.ObservadorEstado;
import powerband.util.DispositivoEntrada;
import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.sintetizadores.IdSintetizador;

public final class PanelInstrumentos extends JPanel {

    private final FachadaControlador controlador;
    private final Consumer<Throwable> manejadorErrores;
    private final DefaultListModel<String> modeloInstrumentos =
            new DefaultListModel<>();
    private final JList<String> listaInstrumentos =
            new JList<>(modeloInstrumentos);
    private final JComboBox<DispositivoEntrada> selectorDispositivo =
            new JComboBox<>();
    private final DefaultListModel<IdSintetizador> modeloSintetizadores =
            new DefaultListModel<>();
    private final JList<IdSintetizador> listaSintetizadores =
            new JList<>(modeloSintetizadores);
    private final JButton botonDispositivo =
            new JButton("Asignar dispositivo");
    private final JButton botonSintetizadores =
            new JButton("Asignar sintetizadores");
    private final JButton botonActualizar = new JButton("Actualizar dispositivos");
    private final JPanel panelTableta = new JPanel(new BorderLayout());
    private final JCheckBox modoDescubrimiento = new JCheckBox(
            "Descubrir lápiz sin emitir sonido", true);
    private final JLabel mensajeEstado =
            new JLabel("Cargando instrumentos...", SwingConstants.LEFT);
    private boolean operacionEnCurso;
    private boolean consultaEnCurso;
    private long versionConsulta;
    private SwingWorker<?, ?> consultaActual;
    private boolean observadorRegistrado;
    private boolean actualizacionPendiente;
    private final ObservadorEstado observador = new ObservadorEstado() {
        @Override
        public void estadoCambiado(EstadoControlador estado) {
        }

        @Override
        public void errorNotificado(String mensaje, Throwable causa) {
        }

        @Override
        public void dispositivoDesconectado(String instrumento) {
            SwingUtilities.invokeLater(() -> {
                if (!Objects.equals(instrumento, listaInstrumentos.getSelectedValue())) {
                    return;
                }
                if (operacionEnCurso) {
                    actualizacionPendiente = true;
                } else {
                    cargarSeleccion();
                }
            });
        }
    };

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
        botonActualizar.addActionListener(evento -> cargarSeleccion());
        modoDescubrimiento.addActionListener(evento ->
                controlador.establecerModoDescubrimientoTableta(modoDescubrimiento.isSelected()));
        selectorDispositivo.addActionListener(evento ->
                actualizarDisponibilidad(listaInstrumentos.getSelectedIndex() >= 0));
        selectorDispositivo.setMaximumRowCount(8);

        actualizarCatalogo();
    }

    private static final class RenderizadorDispositivo
            extends javax.swing.JLabel
            implements javax.swing.ListCellRenderer<DispositivoEntrada> {

        RenderizadorDispositivo() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                javax.swing.JList<? extends DispositivoEntrada> lista,
                DispositivoEntrada valor, int indice, boolean seleccionado,
                boolean focusEnCelda) {
            if (valor != null) {
                setText(valor.nombre());
            } else {
                setText("");
            }
            if (seleccionado) {
                setBackground(ConstantesGUI.COLOR_ACENTO);
                setForeground(ConstantesGUI.COLOR_BLANCO);
            } else {
                setBackground(ConstantesGUI.COLOR_BLANCO);
                setForeground(ConstantesGUI.COLOR_TEXTO);
            }
            setFont(lista.getFont());
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return this;
        }
    }

    protected static final class RenderizadorDispositivoPublico
            extends javax.swing.JLabel
            implements javax.swing.ListCellRenderer<DispositivoEntrada> {

        public RenderizadorDispositivoPublico() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                javax.swing.JList<? extends DispositivoEntrada> lista,
                DispositivoEntrada valor, int indice, boolean seleccionado,
                boolean focusEnCelda) {
            if (valor != null) {
                setText(valor.nombre());
            } else {
                setText("");
            }
            if (seleccionado) {
                setBackground(ConstantesGUI.COLOR_ACENTO);
                setForeground(ConstantesGUI.COLOR_BLANCO);
            } else {
                setBackground(ConstantesGUI.COLOR_BLANCO);
                setForeground(ConstantesGUI.COLOR_TEXTO);
            }
            setFont(lista.getFont());
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            return this;
        }
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected JList<String> listaInstrumentos() {
        return listaInstrumentos;
    }

    protected JComboBox<DispositivoEntrada> selectorDispositivo() {
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

    protected SwingWorker<?, ?> consultaActual() {
        return consultaActual;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (selectorDispositivo.getRenderer() == null
                || !(selectorDispositivo.getRenderer() instanceof RenderizadorDispositivo)) {
            selectorDispositivo.setRenderer(new RenderizadorDispositivo());
            selectorDispositivo.setMaximumRowCount(8);
            selectorDispositivo.setPreferredSize(new Dimension(450, 24));
        }
        if (!observadorRegistrado) {
            controlador.registrarObservadorEstado(observador);
            observadorRegistrado = true;
        }
    }

    @Override
    public void removeNotify() {
        if (observadorRegistrado) {
            controlador.retirarObservadorEstado(observador);
            observadorRegistrado = false;
        }
        super.removeNotify();
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
        dispositivo.add(botonActualizar);
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

        JPanel indicaciones = new JPanel(new BorderLayout());
        indicaciones.add(new JLabel(
                "<html>Toque la superficie con el lápiz y actualice los dispositivos."
                        + "<br>Wintab no garantiza detectar una desconexión física.</html>"),
                BorderLayout.NORTH);
        indicaciones.add(modoDescubrimiento, BorderLayout.SOUTH);
        panelTableta.add(indicaciones, BorderLayout.NORTH);
        Component superficie = controlador.superficieTableta();
        if (superficie != null) {
            superficie.setPreferredSize(new Dimension(
                    ConstantesGUI.ANCHO_SUPERFICIE_TABLETA,
                    ConstantesGUI.ALTO_SUPERFICIE_TABLETA));
            panelTableta.add(superficie, BorderLayout.CENTER);
        }
        panelTableta.setVisible(false);
        panel.add(panelTableta, BorderLayout.SOUTH);

        return panel;
    }

    private void actualizarCatalogo() {
        ejecutarOperacion(() -> new Catalogo(
                Objects.requireNonNull(
                        controlador.listarInstrumentos(),
                        "listarInstrumentos"),
                Objects.requireNonNull(
                        controlador.listarSintetizadores(),
                        "listarSintetizadores")),
                catalogo -> {
                    modeloInstrumentos.clear();
                    for (String instrumento : catalogo.instrumentos()) {
                        modeloInstrumentos.addElement(instrumento);
                    }
                    modeloSintetizadores.clear();
                    for (IdSintetizador sintetizador
                            : catalogo.sintetizadores()) {
                        modeloSintetizadores.addElement(sintetizador);
                    }
                    if (!modeloInstrumentos.isEmpty()) {
                        listaInstrumentos.setSelectedIndex(0);
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
        String instrumento = listaInstrumentos.getSelectedValue();
        long version = ++versionConsulta;
        panelTableta.setVisible("tableta".equals(instrumento));
        selectorDispositivo.removeAllItems();
        if (instrumento == null) {
            consultaEnCurso = false;
            actualizarDisponibilidad(false);
            return;
        }
        consultaEnCurso = true;
        mensajeEstado.setText("Buscando dispositivos para " + instrumento + "...");
        actualizarDisponibilidad(true);
        SwingWorker<Seleccion, Void> consulta = new SwingWorker<>() {
            @Override
            protected Seleccion doInBackground() {
                // Si es tableta, esperar a que el panel esté renderizado
                if ("tableta".equals(instrumento)) {
                    for (int intento = 0; intento < 50; intento++) {
                        if (panelTableta.isShowing()) {
                            break;
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            LoggerPowerBand.getInstance().registrarExcepcionNoCapturada(
                                    e, OrigenLog.UI);
                            break;
                        }
                    }
                }
                return new Seleccion(
                        controlador.listarDispositivos(instrumento),
                        controlador.dispositivoPorInstrumento(),
                        controlador.sintetizadoresPorInstrumento());
            }

            @Override
            protected void done() {
                if (version != versionConsulta) {
                    return;
                }
                consultaEnCurso = false;
                try {
                    Seleccion seleccion = get();
                    cargarSeleccion(instrumento, seleccion);
                } catch (InterruptedException excepcion) {
                    Thread.currentThread().interrupt();
                    mensajeEstado.setText("No se pudieron detectar los dispositivos.");
                    manejadorErrores.accept(excepcion);
                } catch (ExecutionException excepcion) {
                    mensajeEstado.setText("No se pudieron detectar los dispositivos.");
                    manejadorErrores.accept(excepcion.getCause() == null
                            ? excepcion : excepcion.getCause());
                } finally {
                    actualizarDisponibilidad(true);
                }
            }
        };
        consultaActual = consulta;
        consulta.execute();
    }

    private void cargarSeleccion(
            String instrumento, Seleccion seleccion) {
        String dispositivo = seleccion.dispositivosPorInstrumento().get(instrumento);
        DispositivoEntrada asignado = null;
        selectorDispositivo.removeAllItems();
        for (DispositivoEntrada disponible : seleccion.dispositivos()) {
            selectorDispositivo.addItem(disponible);
            if (disponible.id().equals(dispositivo)) {
                asignado = disponible;
            }
        }
        selectorDispositivo.setSelectedItem(asignado);

        List<IdSintetizador> asignados =
                seleccion.sintetizadoresPorInstrumento().getOrDefault(instrumento, List.of());
        listaSintetizadores.clearSelection();
        for (int indice = 0;
                indice < modeloSintetizadores.size();
                indice++) {
            if (asignados.contains(
                    modeloSintetizadores.getElementAt(indice))) {
                listaSintetizadores.addSelectionInterval(indice, indice);
            }
        }
        mensajeEstado.setText(seleccion.dispositivos().isEmpty()
                ? "No hay dispositivos compatibles para " + instrumento + "."
                : asignado == null
                ? "Instrumento: " + instrumento + " (sin dispositivo)"
                : "Instrumento: " + instrumento
                        + " (dispositivo asignado)");
        actualizarDisponibilidad(!operacionEnCurso);
    }

    private void asignarDispositivo() {
        String instrumento = listaInstrumentos.getSelectedValue();
        Object seleccionado = selectorDispositivo.getSelectedItem();
        if (instrumento == null || !(seleccionado instanceof DispositivoEntrada dispositivo)) {
            return;
        }

        ejecutarOperacion(
                () -> controlador.asignarDispositivo(
                        instrumento, dispositivo.id()),
                asignado -> {
                    if (asignado) {
                        mensajeEstado.setText("tableta".equals(instrumento)
                                ? "Lápiz asignado; conexión física no verificada."
                                : "Dispositivo asignado a " + instrumento + ".");
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
                if (actualizacionPendiente) {
                    actualizacionPendiente = false;
                    cargarSeleccion();
                }
            }
        }.execute();
    }

    private void actualizarDisponibilidad(boolean habilitados) {
        boolean disponible = habilitados && !operacionEnCurso;
        listaInstrumentos.setEnabled(disponible);
        selectorDispositivo.setEnabled(disponible && !consultaEnCurso
                && selectorDispositivo.getItemCount() > 0);
        listaSintetizadores.setEnabled(disponible && !consultaEnCurso);
        botonDispositivo.setEnabled(disponible && !consultaEnCurso
                && selectorDispositivo.getSelectedItem() != null);
        botonSintetizadores.setEnabled(disponible && !consultaEnCurso);
        botonActualizar.setEnabled(disponible && !consultaEnCurso);
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrar(Log.error(
                "Fallo en una operación del panel de instrumentos: "
                        + fallo, OrigenLog.UI, fallo));
    }

    private record Catalogo(
            List<String> instrumentos,
            List<IdSintetizador> sintetizadores) {
    }

    private record Seleccion(
            List<DispositivoEntrada> dispositivos,
            Map<String, String> dispositivosPorInstrumento,
            Map<String, List<IdSintetizador>> sintetizadoresPorInstrumento) {
    }
}
