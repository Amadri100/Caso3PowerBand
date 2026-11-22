package powerband.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import powerband.controlador.FachadaControlador;

public final class PanelPrincipal extends JPanel {

    private final FachadaControlador controlador;
    private final JPanel contenido = new JPanel(new CardLayout());
    private final Map<Vista, JPanel> paneles = new EnumMap<>(Vista.class);
    private final Map<Vista, JButton> botonesNavegacion =
            new EnumMap<>(Vista.class);
    private final JLabel tituloVista = new JLabel();
    private final BarraEstado barraEstado;
    private Vista vistaActual = Vista.INICIO;

    public PanelPrincipal(FachadaControlador controlador) {
        this.controlador = Objects.requireNonNull(
                controlador, "controlador");
        setLayout(new BorderLayout());
        setBackground(ConstantesGUI.COLOR_FONDO);

        registrarPaneles();
        barraEstado = new BarraEstado(controlador);
        add(crearNavegacion(), BorderLayout.WEST);
        add(crearAreaContenido(), BorderLayout.CENTER);
        add(barraEstado, BorderLayout.SOUTH);
        mostrarVista(Vista.INICIO);
    }

    protected FachadaControlador fachada() {
        return controlador;
    }

    protected Vista vistaActual() {
        return vistaActual;
    }

    protected  JPanel panelVista(Vista vista) {
        return paneles.get(vista);
    }

    protected JButton botonNavegacion(Vista vista) {
        return botonesNavegacion.get(vista);
    }

    protected int cantidadVistas() {
        return paneles.size();
    }

    public final void limpiarEstado() {
        barraEstado.limpiar();
    }

    protected BarraEstado barraEstado() {
        return barraEstado;
    }

    private void registrarPaneles() {
        for (Vista vista : Vista.values()) {
            JPanel panel;
            if (vista == Vista.INICIO) {
                panel = crearInicio();
            } else if (vista == Vista.CONEXION) {
                panel = new PanelConexion(controlador);
            } else if (vista == Vista.GRABACION) {
                panel = new PanelGrabacion(controlador);
            } else if (vista == Vista.SESIONES) {
                panel = new PanelSesiones(controlador);
            } else if (vista == Vista.CONFIGURACION) {
                panel = new PanelConfiguracion(controlador);
            } else {
                panel = crearMarcador(vista);
            }
            paneles.put(vista, panel);
            contenido.add(panel, vista.name());
        }
    }

    private JPanel crearNavegacion() {
        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBackground(ConstantesGUI.COLOR_BARRA);
        barra.setBorder(BorderFactory.createEmptyBorder(
                ConstantesGUI.MARGEN_BARRA_SUPERIOR,
                ConstantesGUI.MARGEN_BARRA_HORIZONTAL,
                ConstantesGUI.MARGEN_BARRA_INFERIOR,
                ConstantesGUI.MARGEN_BARRA_HORIZONTAL));
        barra.setPreferredSize(new Dimension(
                ConstantesGUI.ANCHO_BARRA_NAVEGACION, 0));

        JLabel marca = new JLabel("PowerBand");
        marca.setForeground(ConstantesGUI.COLOR_BLANCO);
        marca.setFont(marca.getFont().deriveFont(
                Font.BOLD, ConstantesGUI.TAMANO_FUENTE_MARCA));
        marca.setAlignmentX(LEFT_ALIGNMENT);
        barra.add(marca);
        barra.add(Box.createVerticalStrut(
                ConstantesGUI.ESPACIADO_MARCA_MENU));

        JLabel etiquetaMenu = new JLabel("NAVEGACIÓN");
        etiquetaMenu.setForeground(ConstantesGUI.COLOR_TEXTO_MENU);
        etiquetaMenu.setFont(etiquetaMenu.getFont().deriveFont(
                Font.BOLD, ConstantesGUI.TAMANO_FUENTE_MENU));
        etiquetaMenu.setAlignmentX(LEFT_ALIGNMENT);
        barra.add(etiquetaMenu);
        barra.add(Box.createVerticalStrut(
                ConstantesGUI.ESPACIADO_ETIQUETA_MENU));

        for (Vista vista : Vista.values()) {
            JButton boton = new JButton(vista.etiqueta());
            boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.setAlignmentX(LEFT_ALIGNMENT);
            boton.setMaximumSize(new Dimension(
                    Integer.MAX_VALUE,
                    ConstantesGUI.ALTO_BOTON_NAVEGACION));
            boton.setBorder(BorderFactory.createEmptyBorder(
                    ConstantesGUI.MARGEN_BOTON_NAV_VERTICAL,
                    ConstantesGUI.MARGEN_BOTON_NAV_HORIZONTAL,
                    ConstantesGUI.MARGEN_BOTON_NAV_VERTICAL,
                    ConstantesGUI.MARGEN_BOTON_NAV_HORIZONTAL));
            boton.setFocusPainted(false);
            boton.addActionListener(evento -> mostrarVista(vista));
            botonesNavegacion.put(vista, boton);
            barra.add(boton);
            barra.add(Box.createVerticalStrut(
                    ConstantesGUI.ESPACIADO_MENU));
        }

        return barra;
    }

    private JPanel crearAreaContenido() {
        JPanel area = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_AREA_CONTENIDO));
        area.setBackground(ConstantesGUI.COLOR_FONDO);
        area.setBorder(BorderFactory.createEmptyBorder(
                ConstantesGUI.MARGEN_AREA_SUPERIOR,
                ConstantesGUI.MARGEN_AREA_HORIZONTAL,
                ConstantesGUI.MARGEN_AREA_INFERIOR,
                ConstantesGUI.MARGEN_AREA_HORIZONTAL));

        tituloVista.setForeground(ConstantesGUI.COLOR_TEXTO);
        tituloVista.setFont(tituloVista.getFont().deriveFont(
                Font.BOLD, ConstantesGUI.TAMANO_FUENTE_TITULO_VISTA));
        area.add(tituloVista, BorderLayout.NORTH);
        area.add(contenido, BorderLayout.CENTER);
        return area;
    }

    private JPanel crearInicio() {
        JPanel inicio = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_INICIO));
        inicio.setBackground(ConstantesGUI.COLOR_FONDO);

        JLabel introduccion = new JLabel(
                "Accede a las áreas principales de PowerBand.");
        introduccion.setForeground(ConstantesGUI.COLOR_TEXTO);
        introduccion.setFont(introduccion.getFont().deriveFont(
                ConstantesGUI.TAMANO_FUENTE_INTRODUCCION));
        inicio.add(introduccion, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(
                0, ConstantesGUI.COLUMNAS_TARJETAS_INICIO,
                ConstantesGUI.ESPACIADO_TARJETAS,
                ConstantesGUI.ESPACIADO_TARJETAS));
        tarjetas.setBackground(ConstantesGUI.COLOR_FONDO);
        List<Vista> areasPrincipales = List.of(
                Vista.CONEXION,
                Vista.DISPOSITIVOS_INSTRUMENTOS,
                Vista.SINTETIZADORES,
                Vista.GRABACION,
                Vista.SESIONES);
        for (Vista vista : areasPrincipales) {
            tarjetas.add(crearTarjeta(vista));
        }

        JPanel cuerpo = new JPanel(new BorderLayout(
                0, ConstantesGUI.ESPACIADO_CUERPO_INICIO));
        cuerpo.setBackground(ConstantesGUI.COLOR_FONDO);
        cuerpo.add(tarjetas, BorderLayout.CENTER);
        cuerpo.add(crearTarjeta(Vista.CONFIGURACION),
                BorderLayout.SOUTH);
        inicio.add(new JScrollPane(cuerpo), BorderLayout.CENTER);
        return inicio;
    }

    private JPanel crearTarjeta(Vista vista) {
        JPanel tarjeta = new JPanel(new BorderLayout(
                ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_HORIZONTAL,
                ConstantesGUI.ESPACIADO_INTERIOR_TARJETA_VERTICAL));
        tarjeta.setBackground(ConstantesGUI.COLOR_BLANCO);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL,
                        ConstantesGUI.MARGEN_TARJETA_VERTICAL,
                        ConstantesGUI.MARGEN_TARJETA_HORIZONTAL)));

        JLabel titulo = new JLabel(vista.etiqueta());
        titulo.setForeground(ConstantesGUI.COLOR_TEXTO);
        titulo.setFont(titulo.getFont().deriveFont(
                Font.BOLD, ConstantesGUI.TAMANO_FUENTE_TITULO_TARJETA));
        tarjeta.add(titulo, BorderLayout.NORTH);

        JLabel descripcion = new JLabel(vista.descripcion());
        descripcion.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
        tarjeta.add(descripcion, BorderLayout.CENTER);

        JButton abrir = new JButton("Abrir " + vista.etiqueta());
        abrir.setForeground(ConstantesGUI.COLOR_ACENTO);
        abrir.addActionListener(evento -> mostrarVista(vista));
        tarjeta.add(abrir, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel crearMarcador(Vista vista) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ConstantesGUI.COLOR_BLANCO);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ConstantesGUI.COLOR_BORDE),
                BorderFactory.createEmptyBorder(
                        ConstantesGUI.MARGEN_MARCADOR_VERTICAL,
                        ConstantesGUI.MARGEN_MARCADOR_HORIZONTAL,
                        ConstantesGUI.MARGEN_MARCADOR_VERTICAL,
                        ConstantesGUI.MARGEN_MARCADOR_HORIZONTAL)));

        JLabel mensaje = new JLabel(
                "<html><div style='text-align:center'>"
                        + vista.descripcion()
                        + "<br>Este panel se completará en "
                        + vista.tareaResponsable()
                        + ".</div></html>",
                SwingConstants.CENTER);
        mensaje.setForeground(ConstantesGUI.COLOR_TEXTO_SECUNDARIO);
        mensaje.setFont(mensaje.getFont().deriveFont(
                ConstantesGUI.TAMANO_FUENTE_MARCADOR));
        panel.add(mensaje, BorderLayout.CENTER);
        return panel;
    }

    private void mostrarVista(Vista vista) {
        vistaActual = Objects.requireNonNull(vista, "vista");
        tituloVista.setText(vista.etiqueta());
        ((CardLayout) contenido.getLayout()).show(
                contenido, vista.name());
        botonesNavegacion.forEach((destino, boton) -> {
            boolean seleccionada = destino == vista;
            boton.setForeground(seleccionada
                    ? ConstantesGUI.COLOR_BLANCO
                    : ConstantesGUI.COLOR_TEXTO_MENU_INACTIVO);
            boton.setBackground(seleccionada
                    ? ConstantesGUI.COLOR_ACENTO
                    : ConstantesGUI.COLOR_BARRA);
            boton.setOpaque(seleccionada);
        });
    }
}

    