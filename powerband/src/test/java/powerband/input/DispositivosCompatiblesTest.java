package powerband.input;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.Mixer;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import org.junit.jupiter.api.Test;

import powerband.comandos.Comando;
import powerband.instrumentos.Control;
import powerband.instrumentos.Tableta;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispositivosCompatiblesTest {

    @Test
    void admiteSoloControlesTipadosConComponentesYExcluyeIdentidadesAmbiguas() {
        Controller mando = controlador("mando", Controller.Type.GAMEPAD, true);
        Controller desconocido = controlador("desconocido", Controller.Type.UNKNOWN, true);
        Controller raton = controlador("tableta", Controller.Type.MOUSE, true);
        Controller vacio = controlador("vacio", Controller.Type.STICK, false);
        Controller repetido1 = controlador("repetido", Controller.Type.GAMEPAD, true);
        Controller repetido2 = controlador("repetido", Controller.Type.GAMEPAD, true);

        List<DispositivoEntrada> resultado = EntradaControl.dispositivosCompatibles(
                new Controller[] {mando, desconocido, raton, vacio, repetido1, repetido2});

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).nombre().startsWith("mando"));
        assertFalse(EntradaControl.esDispositivoValido(raton));
    }

    @Test
    void distingueMicrofonosConMismoNombreYExcluyeMetadatosIdenticos() {
        Mixer.Info primero = new Info("Microfono", "Proveedor A");
        Mixer.Info segundo = new Info("Microfono", "Proveedor B");
        Mixer.Info repetido1 = new Info("Duplicado", "Proveedor A");
        Mixer.Info repetido2 = new Info("Duplicado", "Proveedor A");
        Mixer.Info incompatible = new Info("Salida", "Proveedor A");

        List<DispositivoEntrada> resultado = EntradaMicrofono.dispositivosCompatibles(
                List.of(primero, segundo, repetido1, repetido2, incompatible),
                info -> info != incompatible);

        assertEquals(2, resultado.size());
        assertFalse(resultado.get(0).id().equals(resultado.get(1).id()));
        assertTrue(resultado.stream().allMatch(dispositivo -> dispositivo.nombre().contains("Microfono")));
    }

    @Test
    void distingueRetiradaDeIdentidadAmbiguaEnControlYMicrofono() {
        Controller control = controlador("mando", Controller.Type.GAMEPAD, true);
        String idControl = EntradaControl.dispositivosCompatibles(new Controller[] {control}).get(0).id();
        assertEquals(DisponibilidadDispositivo.NO_DISPONIBLE,
                EntradaControl.comprobarDisponibilidad(idControl, new Controller[0]));
        assertEquals(DisponibilidadDispositivo.AMBIGUO,
                EntradaControl.comprobarDisponibilidad(idControl, new Controller[] {
                        control, controlador("mando", Controller.Type.GAMEPAD, true)}));

        Mixer.Info microfono = new Info("Microfono", "Proveedor");
        String idMicrofono = EntradaMicrofono.dispositivosCompatibles(
                List.of(microfono), info -> true).get(0).id();
        assertEquals(DisponibilidadDispositivo.NO_DISPONIBLE,
                EntradaMicrofono.comprobarDisponibilidad(idMicrofono, List.of(), info -> true));
        assertEquals(DisponibilidadDispositivo.AMBIGUO,
                EntradaMicrofono.comprobarDisponibilidad(idMicrofono,
                        List.of(microfono, new Info("Microfono", "Proveedor")), info -> true));
        assertEquals(DisponibilidadDispositivo.DISPONIBLE,
                EntradaMicrofono.comprobarDisponibilidad(idMicrofono, List.of(microfono), info -> true));
    }

    @Test
    void noOfreceControlesConIdentidadAmbiguaAunqueUnoNoResponda() {
        Controller disponible = controlador("mando", Controller.Type.GAMEPAD, true);
        Controller ausente = controlador("mando", Controller.Type.GAMEPAD, true, false);

        assertTrue(EntradaControl.dispositivosCompatibles(
                new Controller[] {disponible, ausente}).isEmpty());
    }

    @Test
    void descubrimientoNoEmiteYLaCapturaFiltraPorIdentidadYSuperficie() {
        List<List<Double>> muestras = new ArrayList<>();
        Tableta instrumento = new Tableta() {
            @Override
            public Comando convertirPosicion(double x, double y, double presion) {
                muestras.add(List.of(x, y, presion));
                return null;
            }
        };
        EntradaTableta entrada = new EntradaTableta(instrumento);
        entrada.seleccionarDispositivo("lapiz-a");
        entrada.iniciar();
        try {
            entrada.procesarMuestra("lapiz-a", 25, 50, 0.5f, 100, 100);
            assertTrue(muestras.isEmpty());
            entrada.establecerModoDescubrimiento(false);
            entrada.procesarMuestra("lapiz-b", 25, 50, 0.5f, 100, 100);
            entrada.procesarMuestra("lapiz-a", 101, 50, 0.5f, 100, 100);
            assertTrue(muestras.isEmpty());
            entrada.procesarMuestra("lapiz-a", 25, 50, 0.5f, 100, 100);
            assertEquals(List.of(List.of(0.25, 0.5, 0.5)), muestras);
        } finally {
            entrada.detener();
        }
    }

    @Test
    void convierteLosComponentesDeControlARangosValidos() {
        List<List<Double>> muestras = new ArrayList<>();
        Control instrumento = new Control() {
            @Override
            public Comando convertirEvento(double eje, double valor) {
                muestras.add(List.of(eje, valor));
                return null;
            }
        };
        Component eje = componente(true);
        Component boton = componente(false);
        EntradaControl entrada = new EntradaControl(instrumento);

        entrada.procesarEvento(new Component[] {eje, boton}, eje, -1.0f);
        entrada.procesarEvento(new Component[] {eje, boton}, boton, 1.0f);

        assertEquals(List.of(List.of(0.0, 0.0), List.of(1.0, 1.0)), muestras);
    }

    private static Component componente(boolean analogico) {
        return (Component) Proxy.newProxyInstance(Component.class.getClassLoader(),
                new Class<?>[] {Component.class}, (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "equals" -> proxy == argumentos[0];
                    case "isAnalog" -> analogico;
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }

    private static Controller controlador(String nombre, Controller.Type tipo, boolean componentes) {
        return controlador(nombre, tipo, componentes, true);
    }

    private static Controller controlador(String nombre, Controller.Type tipo,
            boolean componentes, boolean responde) {
        Component eje = (Component) Proxy.newProxyInstance(Component.class.getClassLoader(),
                new Class<?>[] {Component.class}, (proxy, metodo, argumentos) -> {
                    if ("getIdentifier".equals(metodo.getName())) {
                        return Component.Identifier.Axis.X;
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
        return (Controller) Proxy.newProxyInstance(Controller.class.getClassLoader(),
                new Class<?>[] {Controller.class}, (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getName" -> nombre;
                    case "getType" -> tipo;
                    case "poll" -> responde;
                    case "getComponents" -> componentes ? new Component[] {eje} : new Component[0];
                    default -> throw new UnsupportedOperationException(metodo.getName());
                });
    }

    private static final class Info extends Mixer.Info {
        private Info(String nombre, String proveedor) {
            super(nombre, proveedor, "captura", "1");
        }
    }
}
