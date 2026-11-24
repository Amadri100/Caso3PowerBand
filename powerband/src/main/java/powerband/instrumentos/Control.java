package powerband.instrumentos;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoPuntual;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

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
        validarValorNormalizado("eje", eje);
        validarValorNormalizado("valor", valor);
        int nota = (int) Math.round(
                eje * Constantes.NOTA_MIDI_MAXIMA);
        int duracionMs = Math.max(
                Constantes.DURACION_MINIMA_MS,
                (int) Math.round(
                        valor * Constantes.DURACION_MAXIMA_CONTROL_MS));
        Comando comando = new ComandoPuntual(
                destino, nota, duracionMs);
        enviarComando(comando);
        return comando;
    }

    public Comando procesarEvento(double eje, double valor) {
        return convertirEvento(eje, valor);
    }

    private static void validarValorNormalizado(
            String nombre, double valor) {
        if (valor < Constantes.VALOR_MUSICAL_MINIMO
                || valor > Constantes.VALOR_MUSICAL_MAXIMO) {
            throw new IllegalArgumentException(
                    "El " + nombre + " del control debe estar entre 0 y 1");
        }
    }
}
