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
        validarAmplitud(amplitud);
        double notaMidiDouble = frecuenciaAMidi(frecuenciaHz);
        long notaRedondeada = Math.round(notaMidiDouble);
        if (!Double.isFinite(notaMidiDouble)
                || notaRedondeada < Constantes.NOTA_MIDI_MINIMA
                || notaRedondeada > Constantes.NOTA_MIDI_MAXIMA) {
            IllegalArgumentException fallo = new IllegalArgumentException(
                    "La frecuencia produce una nota MIDI fuera de rango: "
                            + notaMidiDouble);
            registrarError("No se pudo convertir el evento del microfono", fallo);
            throw fallo;
        }
        int notaMidi = (int) notaRedondeada;
        Comando comando = new ComandoPuntual(destino,
                notaMidi, Constantes.DURACION_NOTA_ENTRADA_MS);
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

    private void validarAmplitud(double amplitud) {
        if (!Double.isFinite(amplitud)
                || amplitud < Constantes.AMPLITUD_MINIMA
                || amplitud > Constantes.AMPLITUD_MAXIMA) {
            IllegalArgumentException fallo = new IllegalArgumentException(
                    "La amplitud del microfono debe estar entre 0 y 1");
            registrarError("No se pudo convertir el evento del microfono", fallo);
            throw fallo;
        }
    }
}
