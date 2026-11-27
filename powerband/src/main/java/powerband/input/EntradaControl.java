package powerband.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import powerband.instrumentos.Control;
import powerband.util.Constantes;
import powerband.util.DispositivoEntrada;

public class EntradaControl extends Entrada {

    private final Control control;
    private volatile Controller dispositivo;
    private volatile String nombreDispositivo;

    public EntradaControl() {
        this((Control) null, null, null);
    }

    public EntradaControl(Control control) {
        this(control, null, null);
    }

    public EntradaControl(Control control, String nombreDispositivo) {
        this(control, nombreDispositivo, null);
    }

    public EntradaControl(Control control, String nombreDispositivo,
                          Consumer<Entrada> callbackDesconexion) {
        super("control", callbackDesconexion);
        this.control = control;
        this.nombreDispositivo = nombreDispositivo;
    }

    public static List<String> detectarDispositivos() {
        List<String> nombres = new ArrayList<>();
        Controller[] controladores =
                ControllerEnvironment.getDefaultEnvironment()
                        .getControllers();
        for (Controller controlador : controladores) {
            if (esDispositivoValido(controlador)) {
                nombres.add(controlador.getName());
            }
        }
        return List.copyOf(nombres);
    }

    public static List<String> listarDispositivos() {
        return detectarDispositivos();
    }

    public static List<String> detectarControladores() {
        return detectarDispositivos();
    }

    public static boolean excluirTecladoOMouse(String nombre) {
        return esDispositivoValido(nombre);
    }

    public static boolean esDispositivoValido(String nombre) {
        if (nombre == null) {
            return false;
        }
        String nombreNormalizado = nombre.toLowerCase(Locale.ROOT);
        return !nombreNormalizado.contains("keyboard")
                && !nombreNormalizado.contains("teclado")
                && !nombreNormalizado.contains("mouse")
                && !nombreNormalizado.contains("raton")
                && !nombreNormalizado.contains("ratón");
    }

    public static boolean esDispositivoValido(Controller controlador) {
        if (controlador == null) {
            return false;
        }
        Controller.Type tipo = controlador.getType();
        return (tipo == Controller.Type.GAMEPAD || tipo == Controller.Type.STICK)
                && Arrays.stream(controlador.getComponents()).anyMatch(componente ->
                        componente != null && (componente.getIdentifier() instanceof Component.Identifier.Axis
                                || componente.getIdentifier() instanceof Component.Identifier.Button));
    }

    public static List<DispositivoEntrada> dispositivosCompatibles() {
        return dispositivosCompatibles(
                ControllerEnvironment.getDefaultEnvironment().getControllers());
    }

    protected static List<DispositivoEntrada> dispositivosCompatibles(Controller[] controladores) {
        List<Controller> controles = List.of(controladores).stream()
                .filter(EntradaControl::esDispositivoValido).toList();
        Map<String, Long> cantidades = controles.stream()
                .collect(Collectors.groupingBy(EntradaControl::identificador,
                        Collectors.counting()));
        return controles.stream()
                .filter(controlador -> cantidades.get(identificador(controlador)) == 1)
                .filter(EntradaControl::responde)
                .map(controlador -> new DispositivoEntrada(
                        identificador(controlador),
                        controlador.getName() + " (" + controlador.getType() + ")"))
                .toList();
    }

    private static String identificador(Controller controlador) {
        return "control:" + controlador.getType() + ":" + controlador.getName();
    }

    public void seleccionarDispositivo(String nombre) {
        Objects.requireNonNull(nombre, "nombreDispositivo");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del control no puede estar vacio");
        }
        nombreDispositivo = nombre;
    }

    public void limpiarDispositivoSeleccionado() {
        nombreDispositivo = null;
        dispositivo = null;
    }

    public static boolean dispositivoDisponible(String id) {
        return comprobarDisponibilidad(id) == DisponibilidadDispositivo.DISPONIBLE;
    }

    public static DisponibilidadDispositivo comprobarDisponibilidad(String id) {
        return comprobarDisponibilidad(id,
                ControllerEnvironment.getDefaultEnvironment().getControllers());
    }

    protected static DisponibilidadDispositivo comprobarDisponibilidad(String id, Controller[] controladores) {
        Controller coincidencia = null;
        for (Controller candidato : controladores) {
            if (esDispositivoValido(candidato) && identificador(candidato).equals(id)) {
                if (coincidencia != null) {
                    return DisponibilidadDispositivo.AMBIGUO;
                }
                coincidencia = candidato;
            }
        }
        if (coincidencia == null) {
            return DisponibilidadDispositivo.NO_DISPONIBLE;
        }
        return responde(coincidencia) ? DisponibilidadDispositivo.DISPONIBLE
                : DisponibilidadDispositivo.NO_DISPONIBLE;
    }

    private static boolean responde(Controller controlador) {
        synchronized (controlador) {
            return controlador.poll();
        }
    }

    public void procesarEvento(double eje, double valor) {
        validarValor("eje", eje,
                Constantes.VALOR_MUSICAL_MINIMO,
                Constantes.VALOR_MUSICAL_MAXIMO);
        validarValor("valor", valor,
                Constantes.VALOR_MUSICAL_MINIMO,
                Constantes.VALOR_MUSICAL_MAXIMO);
        if (control != null) {
            control.convertirEvento(eje, valor);
        }
    }

    protected void procesarEvento(Component[] componentes, Component componente, float valor) {
        int indice = Arrays.asList(componentes).indexOf(componente);
        if (indice < 0) {
            throw new IllegalStateException("El evento pertenece a un componente desconocido");
        }
        double eje = componentes.length == 1
                ? 0.0 : (double) indice / (componentes.length - 1);
        double valorNormalizado = componente.isAnalog()
                ? NormalizadorEntrada.normalizarCeroUno(valor, -1.0, 1.0)
                : valor;
        procesarEvento(eje, valorNormalizado);
    }

    @Override
    protected void ejecutarCaptura() throws Exception {
        Controller actual = buscarDispositivo();
        this.dispositivo = actual;
        if (actual == null) {
            throw new IllegalStateException(
                    "No se encontro el dispositivo de control configurado");
        }

        while (estaActiva()) {
            boolean responde;
            synchronized (actual) {
                responde = actual.poll();
                EventQueue cola = actual.getEventQueue();
                Event evento = new Event();
                while (responde && cola.getNextEvent(evento)) {
                    Component componente = evento.getComponent();
                    if (componente == null) {
                        continue;
                    }
                    procesarEvento(actual.getComponents(), componente, evento.getValue());
                }
            }
            if (!responde) {
                desconectar("el control dejo de responder");
                return;
            }
            Thread.sleep(10);
        }
    }

    @Override
    protected void liberarRecursosEspecificos() {
        dispositivo = null;
    }

    private Controller buscarDispositivo() {
        Controller[] controladores =
                ControllerEnvironment.getDefaultEnvironment()
                        .getControllers();
        Controller encontrado = null;
        for (Controller controlador : controladores) {
            if (esDispositivoValido(controlador)
                    && (nombreDispositivo == null
                    || nombreDispositivo.equals(identificador(controlador))
                    || nombreDispositivo.equals(controlador.getName()))) {
                if (encontrado != null && nombreDispositivo != null) {
                    throw new IllegalStateException("El control seleccionado es ambiguo");
                }
                encontrado = controlador;
                if (nombreDispositivo == null) {
                    break;
                }
            }
        }
        return encontrado;
    }
}
