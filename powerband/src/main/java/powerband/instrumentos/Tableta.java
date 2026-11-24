package powerband.instrumentos;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoPuntual;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

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
            rechazarEvento("Los datos de tableta deben ser finitos");
        }
        validarNormalizado("posicionX", posicionX);
        validarNormalizado("posicionY", posicionY);
        validarNormalizado("presion", presion);
        int nota = (int) Math.round(
                posicionX * Constantes.NOTA_MIDI_MAXIMA);
        Comando comando = new ComandoPuntual(
                destino, nota, Constantes.DURACION_NOTA_ENTRADA_MS);
        enviarComando(comando);
        return comando;
    }

    public Comando procesarPosicion(double posicionX,
                                    double posicionY,
                                    double presion) {
        return convertirPosicion(posicionX, posicionY, presion);
    }

    private void validarNormalizado(String nombre, double valor) {
        if (valor < Constantes.VALOR_MUSICAL_MINIMO
                || valor > Constantes.VALOR_MUSICAL_MAXIMO) {
            rechazarEvento("El " + nombre
                    + " de la tableta debe estar entre 0 y 1");
        }
    }

    private void rechazarEvento(String mensaje) {
        IllegalArgumentException fallo = new IllegalArgumentException(mensaje);
        registrarError("No se pudo convertir el evento de la tableta", fallo);
        throw fallo;
    }
}
