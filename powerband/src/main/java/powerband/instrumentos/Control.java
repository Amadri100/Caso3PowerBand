package powerband.instrumentos;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoPuntual;
import powerband.sintetizadores.Sintetizador;

public class Control extends Instrumento {

    public Control() {
        super();
    }

    public Control(Sintetizador sintetizador) {
        super(sintetizador);
    }

    public Control(Sintetizador sintetizador, BusComandos busComandos) {
        super(sintetizador, busComandos);
    }

    public Comando convertirEvento(double eje, double valor) {
        Sintetizador destino = sintetizador();
        if (destino == null) {
            throw new IllegalStateException(
                    "El control no tiene un sintetizador asignado");
        }
        if (!Double.isFinite(eje) || !Double.isFinite(valor)) {
            throw new IllegalArgumentException(
                    "El evento del control debe contener valores finitos");
        }
        Comando comando = new ComandoPuntual(destino, eje, valor);
        enviarComando(comando);
        return comando;
    }

    public Comando procesarEvento(double eje, double valor) {
        return convertirEvento(eje, valor);
    }
}
