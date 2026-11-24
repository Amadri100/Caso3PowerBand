package powerband.instrumentos;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoPuntual;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

public class Microfono extends Instrumento {

    public Microfono() {
        super();
    }

    public Microfono(Sintetizador sintetizador) {
        super(sintetizador);
    }

    public Microfono(Sintetizador sintetizador, BusComandos busComandos) {
        super(sintetizador, busComandos);
    }

    public Comando convertirFrecuencia(double frecuenciaHz,
                                       double amplitud) {
        Sintetizador destino = sintetizador();
        if (destino == null) {
            throw new IllegalStateException(
                    "El microfono no tiene un sintetizador asignado");
        }
        double notaMidiDouble = frecuenciaAMidi(frecuenciaHz);
        int notaMidi = (int) Math.round(notaMidiDouble);
        Comando comando = new ComandoPuntual(destino,
                notaMidi, amplitud);
        enviarComando(comando);
        return comando;
    }

    public Comando procesarFrecuencia(double frecuenciaHz,
                                      double amplitud) {
        return convertirFrecuencia(frecuenciaHz, amplitud);
    }

    public double frecuenciaAMidi(double frecuenciaHz) {
        if (!Double.isFinite(frecuenciaHz) || frecuenciaHz <= 0.0) {
            throw new IllegalArgumentException(
                    "La frecuencia debe ser finita y mayor que cero");
        }
        return 69.0 + Constantes.SEMITONOS_POR_OCTAVA * (
                Math.log(frecuenciaHz
                        / Constantes.FRECUENCIA_AFINACION_HZ)
                        / Math.log(2.0));
    }
}
