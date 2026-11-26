package powerband.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import powerband.controlador.FachadaControlador;
import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;

public final class PanelConfiguracion extends JPanel {

    private final FachadaControlador controlador;
    private final Consumer<Throwable> manejadorErrores;
    private final Map<String, FilaOpcion> filas = new LinkedHashMap<>();
    private final JLabel mensajeVacio = new JLabel(
            "No hay opciones configurables disponibles.");

    public PanelConfiguracion(FachadaControlador controlador) {
        this(controlador, PanelConfiguracion::registrarError);
    }

    protected PanelConfiguracion(
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

        JLabel instrucciones = new JLabel(
                "Consulta y modifica las opciones expuestas por la aplicación.");
        instrucciones.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
        add(instrucciones, BorderLayout.NORTH);

        JPanel listaOpciones = new JPanel();
        listaOpciones.setLayout(new BoxLayout(
                listaOpciones, BoxLayout.Y_AXIS));
        listaOpciones.setBackground(ConstantesGUI.COLOR_FONDO);

        Map<String, Object> opciones = Objects.requireNonNull(
                controlador.opcionesConfigurables(),
                "opcionesConfigurables");
        for (Map.Entry<String, Object> opcion : opciones.entrySet()) {
            String clave = Objects.requireNonNull(
                    opcion.getKey(), "clave de opción");
            if (clave.isBlank()) {
                throw new IllegalArgumentException(
                        "La clave de opción no puede estar vacía");
            }

            FilaOpcion fila = new FilaOpcion(clave, opcion.getValue());
            if (fila.botonGuardar() != null) {
                fila.botonGuardar().addActionListener(
                        evento -> guardarOpcion(fila));
            }
            filas.put(clave, fila);
            listaOpciones.add(fila);
        }

        if (filas.isEmpty()) {
            mensajeVacio.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
            add(mensajeVacio, BorderLayout.CENTER);
        } else {
            add(new JScrollPane(listaOpciones), BorderLayout.CENTER);
        }
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected Component editorOpcion(String clave) {
        FilaOpcion fila = filas.get(clave);
        return fila == null ? null : fila.editor();
    }

    protected JButton botonGuardar(String clave) {
        FilaOpcion fila = filas.get(clave);
        return fila == null ? null : fila.botonGuardar();
    }

    protected JLabel mensajeVacio() {
        return filas.isEmpty() ? mensajeVacio : null;
    }

    private void guardarOpcion(FilaOpcion fila) {
        try {
            Object nuevoValor = fila.leerValor();
            if (Objects.equals(fila.valorConfirmado(), nuevoValor)) {
                return;
            }
            controlador.establecerOpcion(fila.clave(), nuevoValor);
            fila.confirmar(nuevoValor);
        } catch (RuntimeException fallo) {
            fila.restaurarValorConfirmado();
            manejadorErrores.accept(fallo);
        }
    }

    private static void registrarError(Throwable fallo) {
        LoggerPowerBand.getInstance().registrar(Log.error(
                "Fallo al guardar una opción de configuración: " + fallo,
                OrigenLog.UI, fallo));
    }

    private static final class FilaOpcion extends JPanel {

        private final String clave;
        private final Object valorInicial;
        private final Component editor;
        private final JButton botonGuardar;
        private Object valorConfirmado;

        private FilaOpcion(String clave, Object valor) {
            this.clave = clave;
            this.valorInicial = valor;
            this.valorConfirmado = valor;
            setLayout(new BorderLayout(
                    ConstantesGUI.ESPACIADO_TARJETAS,
                    ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_VERTICAL));
            setBackground(ConstantesGUI.COLOR_BLANCO);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                    BorderFactory.createEmptyBorder(
                            ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                            ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                            ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                            ConstantesGUI.MARGEN_TARJETA_HORIZONTAL)));

            JLabel etiqueta = new JLabel(clave);
            etiqueta.setForeground(ConstantesGUI.COLOR_TEXTO);
            add(etiqueta, BorderLayout.NORTH);

            JPanel controles = new JPanel(new FlowLayout(
                    FlowLayout.LEFT, ConstantesGUI.ESPACIADO_TARJETAS, 0));
            controles.setBackground(ConstantesGUI.COLOR_BLANCO);
            editor = crearEditor(valor);
            controles.add(editor);
            if (esEditable(valor)) {
                botonGuardar = new JButton("Guardar");
                botonGuardar.setForeground(ConstantesGUI.COLOR_BLANCO);
                botonGuardar.setBackground(ConstantesGUI.COLOR_ACENTO);
                botonGuardar.setFocusPainted(false);
                controles.add(botonGuardar);
            } else {
                botonGuardar = null;
            }
            add(controles, BorderLayout.CENTER);
        }

        private String clave() {
            return clave;
        }

        private Component editor() {
            return editor;
        }

        private JButton botonGuardar() {
            return botonGuardar;
        }

        private Object valorConfirmado() {
            return valorConfirmado;
        }

        private Object leerValor() {
            if (editor instanceof JCheckBox casilla) {
                return casilla.isSelected();
            }

            String texto = ((JTextField) editor).getText();
            if (valorInicial instanceof String) {
                return texto;
            }
            if (valorInicial instanceof Integer) {
                return Integer.valueOf(texto);
            }
            if (valorInicial instanceof Long) {
                return Long.valueOf(texto);
            }
            if (valorInicial instanceof Double) {
                return Double.valueOf(texto);
            }
            throw new IllegalStateException(
                    "La opción no tiene un editor compatible: " + clave);
        }

        private void confirmar(Object valor) {
            valorConfirmado = valor;
        }

        private void restaurarValorConfirmado() {
            if (editor instanceof JCheckBox casilla) {
                casilla.setSelected((Boolean) valorConfirmado);
            } else if (editor instanceof JTextField campo) {
                campo.setText(String.valueOf(valorConfirmado));
            }
        }

        private static boolean esEditable(Object valor) {
            return valor instanceof String
                    || valor instanceof Boolean
                    || valor instanceof Integer
                    || valor instanceof Long
                    || valor instanceof Double;
        }

        private static Component crearEditor(Object valor) {
            if (valor instanceof Boolean valorBooleano) {
                return new JCheckBox("Activada", valorBooleano);
            }
            if (valor instanceof String
                    || valor instanceof Integer
                    || valor instanceof Long
                    || valor instanceof Double) {
                return new JTextField(
                        String.valueOf(valor),
                        ConstantesGUI.COLUMNAS_CAMPO_CONFIGURACION);
            }
            String tipo = valor == null
                    ? "null"
                    : valor.getClass().getSimpleName();
            String valorVisible = String.valueOf(valor);
            return new JLabel(
                    "Valor actual: " + valorVisible
                            + " (tipo " + tipo + "; edición no disponible)");
        }
    }
}
