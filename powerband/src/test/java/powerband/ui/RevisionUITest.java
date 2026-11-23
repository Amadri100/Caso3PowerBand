package powerband.ui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RevisionUITest {

    private static final List<Class<?>> CLASES_UI = List.of(
            PanelPrincipal.class,
            PanelConexion.class,
            PanelGrabacion.class,
            PanelSesiones.class,
            PanelConfiguracion.class,
            PanelInstrumentos.class,
            BarraEstado.class,
            VentanaPrincipal.class);

    private static final List<String> PAQUETES_PROHIBIDOS = List.of(
            "powerband.archivos.",
            "powerband.controlador.Controlador",
            "powerband.controlador.FachadaControladorFalsa",
            "powerband.grabaciones.",
            "powerband.input.",
            "powerband.sintetizadores.BandClient",
            "java.net.",
            "javax.sound.");

    @Test
    public void clasesUINoContienenTiposDeBajoNivel() {
        for (Class<?> clase : CLASES_UI) {
            for (Field campo : clase.getDeclaredFields()) {
                assertFalse(
                        tieneTipoProhibido(campo.getType()),
                        clase.getSimpleName() + " usa "
                                + campo.getType().getName());
            }
            for (Method metodo : clase.getDeclaredMethods()) {
                assertFalse(
                        tieneTipoProhibido(metodo.getReturnType()),
                        clase.getSimpleName() + " devuelve "
                                + metodo.getReturnType().getName());
                for (Parameter parametro : metodo.getParameters()) {
                    assertFalse(
                            tieneTipoProhibido(parametro.getType()),
                            clase.getSimpleName() + " recibe "
                                    + parametro.getType().getName());
                }
            }
        }
    }

    @Test
    public void navegacionNoConservaPanelesSeparadosObsoletos() {
        assertTrue(existeVista("INSTRUMENTOS"));
        assertFalse(existeVista("DISPOSITIVOS_INSTRUMENTOS"));
        assertFalse(existeVista("SINTETIZADORES"));
    }

    @Test
    public void noHaySystemExitEnLaVentana() {
        for (Method metodo
                : VentanaPrincipal.class.getDeclaredMethods()) {
            assertFalse(
                    metodo.toGenericString().contains("System.exit"));
        }
    }

    private static boolean existeVista(String nombre) {
        try {
            Vista.valueOf(nombre);
            return true;
        } catch (IllegalArgumentException excepcion) {
            return false;
        }
    }

    private static boolean tieneTipoProhibido(Class<?> tipo) {
        if (tipo.isArray()) {
            return tieneTipoProhibido(tipo.getComponentType());
        }
        String nombre = tipo.getName();
        for (String prohibido : PAQUETES_PROHIBIDOS) {
            if (nombre.startsWith(prohibido)
                    || nombre.equals(prohibido)) {
                return true;
            }
        }
        return false;
    }
}
