package powerband.sintetizadores;

import java.io.IOException;
import java.util.Objects;

import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

abstract class AdaptadorBandClient implements Sintetizador {

    protected final BandClient bandClient;

    AdaptadorBandClient(BandClient bandClient) {
        this.bandClient = Objects.requireNonNull(bandClient, "bandClient");
    }

    protected final void validarAccion(
            TipoAccion recibida, TipoAccion esperada, String sintetizador) {
        if (recibida != esperada) {
            rechazar(sintetizador + " no admite la accion " + recibida);
        }
    }

    protected final void validarCantidad(
            double[] parametros, int minima, int maxima, String sintetizador) {
        if (parametros == null
                || parametros.length < minima
                || parametros.length > maxima) {
            rechazar("Cantidad de parametros invalida para " + sintetizador);
        }
    }

    protected final int notaMidi(double valor, String sintetizador) {
        return enteroEnRango(valor, Constantes.NOTA_MIDI_MINIMA,
                Constantes.NOTA_MIDI_MAXIMA, "nota MIDI", sintetizador);
    }

    protected final int duracionMs(double valor, String sintetizador) {
        return enteroEnRango(valor, Constantes.DURACION_MINIMA_MS,
                Constantes.DURACION_MAXIMA_MS, "duracion en milisegundos",
                sintetizador);
    }

    protected final int enteroEnRango(
            double valor, int minimo, int maximo, String nombre, String sintetizador) {
        if (!Double.isFinite(valor)
                || valor != Math.rint(valor)
                || valor < minimo
                || valor > maximo) {
            rechazar(nombre + " fuera de rango para " + sintetizador + ": " + valor);
        }
        return (int) valor;
    }

    protected final float razonParcial(double valor, int numeroParcial) {
        float resultado = (float) valor;
        if (!Double.isFinite(valor) || valor <= 0.0
                || !Float.isFinite(resultado) || resultado <= 0.0f) {
            rechazar("La razon del parcial " + numeroParcial
                    + " debe ser positiva y finita");
        }
        return resultado;
    }

    protected final float amplitudParcial(double valor, int numeroParcial) {
        float resultado = (float) valor;
        if (!Double.isFinite(valor)
                || valor < Constantes.AMPLITUD_MINIMA
                || valor > Constantes.AMPLITUD_MAXIMA
                || !Float.isFinite(resultado)) {
            rechazar("La amplitud del parcial " + numeroParcial
                    + " debe estar entre 0 y 1");
        }
        return resultado;
    }

    protected final void enviar(String descripcion, OperacionTransporte operacion) {
        try {
            operacion.ejecutar();
        } catch (IOException fallo) {
            IllegalStateException error = new IllegalStateException(
                    "Fallo local al " + descripcion, fallo);
            registrarError(error);
            throw error;
        }
    }

    protected final void rechazar(String mensaje) {
        IllegalArgumentException error = new IllegalArgumentException(mensaje);
        registrarError(error);
        throw error;
    }

    private static void registrarError(RuntimeException error) {
        try {
            LoggerPowerBand.getInstance().registrarError(error.getMessage());
        } catch (IllegalStateException falloLog) {
            error.addSuppressed(falloLog);
        }
    }

    @FunctionalInterface
    protected interface OperacionTransporte {
        void ejecutar() throws IOException;
    }
}
