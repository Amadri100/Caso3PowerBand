package powerband.sintetizadores;

import java.io.IOException;

import powerband.comandos.TipoAccion;
import powerband.excepciones.FalloTransporteException;
import powerband.logging.Log;
import powerband.logging.LoggerPowerBand;
import powerband.logging.OrigenLog;
import powerband.util.Constantes;

public interface Sintetizador {

    public IdSintetizador id();

    public default BandClient bandClient() {
        return null;
    }

    public default void ejecutarPuntual(double... parametros) {
        ejecutar(TipoAccion.PUNTUAL, parametros);
    }

    public default void ejecutarPuntual(ParametrosNota parametros) {
        ejecutarPuntual(parametros.parametrosPuntuales());
    }

    public default void iniciar(String voiceId, double... parametros) {
        ejecutar(TipoAccion.INICIO, parametros);
    }

    public default void iniciar(
            String voiceId, ParametrosNota parametros) {
        iniciar(voiceId, parametros.parametrosSostenidos());
    }

    public default void actualizar(String voiceId, double... parametros) {
        ejecutar(TipoAccion.ACTUALIZACION, parametros);
    }

    public default void actualizar(
            String voiceId, ParametrosNota parametros) {
        actualizar(voiceId, parametros.parametrosSostenidos());
    }

    public default void finalizar(String voiceId, double... parametros) {
        ejecutar(TipoAccion.FINALIZACION, parametros);
    }

    public default void ejecutar(TipoAccion tipoAccion, double... parametros) {
        switch (tipoAccion) {
            case PUNTUAL -> ejecutarPuntual(parametros);
            case INICIO -> iniciar("inicio", parametros);
            case ACTUALIZACION -> actualizar("actualizacion", parametros);
            case FINALIZACION -> finalizar("finalizacion", parametros);
        }
    }

    public void liberarTodasLasNotas();

    public default void validarAccion(
            TipoAccion recibida, TipoAccion esperada, String sintetizador) {
        if (recibida != esperada) {
            rechazar(sintetizador + " no admite la accion " + recibida);
        }
    }

    public default void validarCantidad(
            double[] parametros, int minima, int maxima, String sintetizador) {
        if (parametros == null
                || parametros.length < minima
                || parametros.length > maxima) {
            rechazar("Cantidad de parametros invalida para " + sintetizador);
        }
    }

    public default int notaMidi(double valor, String sintetizador) {
        return enteroEnRango(valor, Constantes.NOTA_MIDI_MINIMA,
                Constantes.NOTA_MIDI_MAXIMA, "nota MIDI", sintetizador);
    }

    public default int duracionMs(double valor, String sintetizador) {
        return enteroEnRango(valor, Constantes.DURACION_MINIMA_MS,
                Constantes.DURACION_MAXIMA_MS, "duracion en milisegundos",
                sintetizador);
    }

    public default int enteroEnRango(
            double valor, int minimo, int maximo,
            String nombre, String sintetizador) {
        if (!Double.isFinite(valor)
                || valor != Math.rint(valor)
                || valor < minimo
                || valor > maximo) {
            rechazar(nombre + " fuera de rango para " + sintetizador
                    + ": " + valor);
        }
        return (int) valor;
    }

    public default float razonParcial(double valor, int numeroParcial) {
        float resultado = (float) valor;
        if (!Double.isFinite(valor) || valor <= 0.0
                || !Float.isFinite(resultado) || resultado <= 0.0f) {
            rechazar("La razon del parcial " + numeroParcial
                    + " debe ser positiva y finita");
        }
        return resultado;
    }

    public default float amplitudParcial(double valor, int numeroParcial) {
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

    public default void enviar(
            String descripcion, OperacionTransporte operacion) {
        try {
            operacion.ejecutar();
        } catch (IOException fallo) {
            FalloTransporteException error = new FalloTransporteException(
                    "Fallo local al " + descripcion, fallo);
            registrarError(error);
            throw error;
        }
    }

    public default void rechazar(String mensaje) {
        IllegalArgumentException error = new IllegalArgumentException(mensaje);
        registrarError(error);
        throw error;
    }

    public default void registrarError(RuntimeException error) {
        try {
            LoggerPowerBand.getInstance().registrar(
                    Log.error(error.getMessage(),
                            OrigenLog.SINTETIZADORES, error));
        } catch (IllegalStateException falloLog) {
            error.addSuppressed(falloLog);
        }
    }

    @FunctionalInterface
    public interface OperacionTransporte {
        void ejecutar() throws IOException;
    }
}
