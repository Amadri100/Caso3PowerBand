package powerband.ui;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.FachadaControladorFalsa;
import powerband.util.Configuraciones;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelConfiguracionTest {

    @Test
    void activaYDesactivaDebugMedianteLaFachada() throws Exception {
        Configuraciones configuraciones = Configuraciones.getInstance();
        configuraciones.setOpcion(Constantes.OPCION_DEBUG_LOG, false);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PanelConfiguracion panel = new PanelConfiguracion(new FachadaControladorFalsa());
                JCheckBox editor = (JCheckBox) panel.editorOpcion(Constantes.OPCION_DEBUG_LOG);
                assertTrue(!editor.isSelected());
                editor.setSelected(true);
                panel.botonGuardar(Constantes.OPCION_DEBUG_LOG).doClick();
                assertEquals(Boolean.TRUE, configuraciones.getOpcion(Constantes.OPCION_DEBUG_LOG));
                editor.setSelected(false);
                panel.botonGuardar(Constantes.OPCION_DEBUG_LOG).doClick();
                assertEquals(Boolean.FALSE, configuraciones.getOpcion(Constantes.OPCION_DEBUG_LOG));
            });
        } finally {
            configuraciones.setOpcion(Constantes.OPCION_DEBUG_LOG, false);
        }
    }

    @Test
    void muestraEstadoVacioSinEscribirOpcionesAlConstruirse()
            throws Exception {
        AtomicInteger escrituras = new AtomicInteger();
        AtomicInteger consultas = new AtomicInteger();
        FachadaControlador controlador = fachada(Map.of(), (clave, valor) -> {
            escrituras.incrementAndGet();
        }, consultas);
        AtomicReference<PanelConfiguracion> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PanelConfiguracion panel = new PanelConfiguracion(controlador);
            panelReferencia.set(panel);
            assertSame(controlador, panel.fachada());
            assertEquals(
                    "No hay opciones configurables disponibles.",
                    panel.mensajeVacio().getText());
            assertNull(panel.editorOpcion("host"));
        });

        assertEquals(1, consultas.get());
        assertEquals(0, escrituras.get());
    }

    @Test
    void creaEditoresTipadosYDejaLosTiposDesconocidosSoloEnLectura()
            throws Exception {
        Map<String, Object> opciones = new LinkedHashMap<>();
        opciones.put("nombre", "demo");
        opciones.put("activo", Boolean.TRUE);
        opciones.put("canales", 4);
        opciones.put("objeto", new Object());
        FachadaControlador controlador = fachada(
                opciones, (clave, valor) -> { }, new AtomicInteger());

        SwingUtilities.invokeAndWait(() -> {
            PanelConfiguracion panel = new PanelConfiguracion(controlador);

            assertEquals("demo",
                    assertInstanceOf(
                            JTextField.class,
                            panel.editorOpcion("nombre")).getText());
            assertTrue(assertInstanceOf(
                    JCheckBox.class,
                    panel.editorOpcion("activo")).isSelected());
            assertEquals("4",
                    assertInstanceOf(
                            JTextField.class,
                            panel.editorOpcion("canales")).getText());
            assertInstanceOf(
                    JLabel.class, panel.editorOpcion("objeto"));
            assertNull(panel.botonGuardar("objeto"));
            assertTrue(panel.botonGuardar("nombre").isEnabled());
        });
    }

    @Test
    void guardaUnaSolaOpcionYNoReescribeValoresSinCambios()
            throws Exception {
        Map<String, Object> opciones = new LinkedHashMap<>();
        opciones.put("nombre", "inicial");
        opciones.put("canales", 2);
        Map<String, Object> escrituras = new LinkedHashMap<>();
        FachadaControlador controlador = fachada(
                opciones,
                (clave, valor) -> escrituras.put(clave, valor),
                new AtomicInteger());

        SwingUtilities.invokeAndWait(() -> {
            PanelConfiguracion panel = new PanelConfiguracion(controlador);
            JTextField nombre = (JTextField) panel.editorOpcion("nombre");
            nombre.setText("nuevo");
            panel.botonGuardar("nombre").doClick();
            assertEquals("nuevo", nombre.getText());
            assertEquals(1, escrituras.size());
            assertEquals("nuevo", escrituras.get("nombre"));

            panel.botonGuardar("nombre").doClick();
            assertEquals(1, escrituras.size());

            JTextField canales = (JTextField) panel.editorOpcion("canales");
            canales.setText("3");
            panel.botonGuardar("canales").doClick();
            assertEquals(2, escrituras.size());
            assertEquals(Integer.valueOf(3), escrituras.get("canales"));
        });
    }

    @Test
    void restauraElUltimoValorConfirmadoSiFallaLaEdicion()
            throws Exception {
        AtomicReference<Throwable> errorCapturado = new AtomicReference<>();
        FachadaControlador controlador = fachada(
                Map.of("canales", 2),
                (clave, valor) -> {
                    throw new IllegalStateException("fallo al guardar");
                },
                new AtomicInteger());

        SwingUtilities.invokeAndWait(() -> {
            PanelConfiguracion panel = new PanelConfiguracion(
                    controlador, errorCapturado::set);
            JTextField canales = (JTextField) panel.editorOpcion("canales");
            canales.setText("3");
            panel.botonGuardar("canales").doClick();

            assertEquals("2", canales.getText());
            assertNotNull(errorCapturado.get());
            assertInstanceOf(IllegalStateException.class,
                    errorCapturado.get());
            assertEquals("fallo al guardar",
                    errorCapturado.get().getMessage());
        });
    }

    private static FachadaControlador fachada(
            Map<String, Object> opciones,
            EscritorOpcion escritor,
            AtomicInteger consultas) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getName().equals("opcionesConfigurables")) {
                        consultas.incrementAndGet();
                        return opciones;
                    }
                    if (metodo.getName().equals("establecerOpcion")) {
                        escritor.escribir(
                                (String) argumentos[0], argumentos[1]);
                        return null;
                    }
                    if (metodo.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
    }

    @FunctionalInterface
    private interface EscritorOpcion {
        void escribir(String clave, Object valor);
    }
}
