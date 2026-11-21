package powerband.comandos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;

public final class BusComandos {

    private final List<ObservadorComandos> observadores = new ArrayList<>();

    public void registrarObservador(ObservadorComandos observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            boolean yaRegistrado = observadores.stream()
                    .anyMatch(registrado -> registrado == observador);
            if (!yaRegistrado) {
                observadores.add(observador);
            }
        }
    }

    public void quitarObservador(ObservadorComandos observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            observadores.removeIf(registrado -> registrado == observador);
        }
    }

    public void publicar(Comando comando, long momentoNanos) {
        Objects.requireNonNull(comando, "comando");

        List<ObservadorComandos> copiaObservadores;
        synchronized (observadores) {
            copiaObservadores = List.copyOf(observadores);
        }

        List<RuntimeException> fallos = new ArrayList<>();
        for (ObservadorComandos observador : copiaObservadores) {
            try {
                boolean procesado =
                        observador.comandoEnviado(comando, momentoNanos);
                if (!procesado) {
                    fallos.add(new IllegalStateException(
                            "El observador no pudo procesar el comando"));
                }
            } catch (RuntimeException fallo) {
                fallos.add(fallo);
            }
        }

        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        for (RuntimeException fallo : fallos) {
            logger.registrarError("Fallo al notificar un comando a un observador: "
                    + fallo);
        }
    }
}
