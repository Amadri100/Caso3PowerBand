package powerband.instrumentos;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoPuntual;
import powerband.sintetizadores.Sintetizador;

public class Tableta extends Instrumento {

    public Tableta() {
        super();
    }

    public Tableta(Sintetizador sintetizador) {
        super(sintetizador);
    }

    public Tableta(Sintetizador sintetizador, BusComandos busComandos) {
        super(sintetizador, busComandos);
    }

    public Comando convertirPosicion(double posicionX,
                                     double posicionY,
                                     double presion) {
        Sintetizador destino = sintetizador();
        if (destino == null) {
            throw new IllegalStateException(
                    "La tableta no tiene un sintetizador asignado");
        }
        if (!Double.isFinite(posicionX)
                || !Double.isFinite(posicionY)
                || !Double.isFinite(presion)) {
            throw new IllegalArgumentException(
                    "Los datos de tableta deben ser finitos");
        }
        Comando comando = new ComandoPuntual(
                destino, posicionX, posicionY, presion);
        enviarComando(comando);
        return comando;
    }

    public Comando procesarPosicion(double posicionX,
                                    double posicionY,
                                    double presion) {
        return convertirPosicion(posicionX, posicionY, presion);
    }
}
