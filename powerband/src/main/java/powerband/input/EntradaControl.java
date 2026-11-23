package powerband.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import powerband.instrumentos.Control;
import powerband.util.Constantes;

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
        if (tipo == Controller.Type.KEYBOARD
                || tipo == Controller.Type.MOUSE) {
            return false;
        }
        return esDispositivoValido(controlador.getName());
    }

    public void seleccionarDispositivo(String nombre) {
        Objects.requireNonNull(nombre, "nombreDispositivo");
        if (!esDispositivoValido(nombre)) {
            throw new IllegalArgumentException(
                    "Teclado y mouse no son dispositivos validos");
        }
        nombreDispositivo = nombre;
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

    @Override
    protected void ejecutarCaptura() throws Exception {
        dispositivo = buscarDispositivo();
        if (dispositivo == null) {
            throw new IllegalStateException(
                    "No se encontro el dispositivo de control configurado");
        }

        while (estaActiva()) {
            if (!dispositivo.poll()) {
                desconectar("el control dejo de responder");
                return;
            }
            EventQueue cola = dispositivo.getEventQueue();
            Event evento = new Event();
            while (cola.getNextEvent(evento)) {
                Component componente = evento.getComponent();
                if (componente == null) {
                    continue;
                }
                float valor = evento.getValue();
                double valorNormalizado =
                        NormalizadorEntrada.normalizarCeroUno(
                                valor, -1.0, 1.0);
                procesarEvento(
                        componente.getIdentifier().hashCode(),
                        valorNormalizado);
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
        for (Controller controlador : controladores) {
            if (esDispositivoValido(controlador)
                    && (nombreDispositivo == null
                    || nombreDispositivo.equals(controlador.getName()))) {
                return controlador;
            }
        }
        return null;
    }
}
