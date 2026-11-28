package powerband.comandos;

import java.util.Arrays;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.sintetizadores.Sintetizador;
import powerband.sintetizadores.ParametrosNota;

public final class ComandoPuntual implements Comando {

    private final Sintetizador destino;
    private final double[] parametros;
    private final ParametrosNota parametrosNota;

    public ComandoPuntual(Sintetizador destino, double... parametros) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.parametros = parametros == null
                ? new double[0]
                : parametros.clone();
        this.parametrosNota = ParametrosNota.desdeParametrosPuntuales(this.parametros);
    }

    public ComandoPuntual(
            Sintetizador destino, ParametrosNota parametrosNota) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.parametrosNota = Objects.requireNonNull(
                parametrosNota, "parametrosNota");
        this.parametros = parametrosNota.parametrosPuntuales();
    }

    public ComandoPuntual(Sintetizador destino, TipoAccion tipoAccion,
                          double... parametros) {
        this(destino, parametros);
        if (tipoAccion != TipoAccion.PUNTUAL) {
            throw new IllegalArgumentException(
                    "ComandoPuntual requiere la accion PUNTUAL");
        }
    }

    @Override
    public void ejecutar() {
        destino.ejecutarPuntual(parametrosNota);
        LoggerPowerBand.getInstance().registrar(Log.comando(this, OrigenLog.COMANDOS));
    }

    @Override
    public Sintetizador destino() {
        return destino;
    }

    @Override
    public TipoAccion tipoAccion() {
        return TipoAccion.PUNTUAL;
    }

    @Override
    public double[] parametros() {
        return parametros.clone();
    }

    @Override
    public ParametrosNota parametrosNota() {
        return parametrosNota;
    }

    @Override
    public String toString() {
        return "ComandoPuntual{destino=" + destino.id()
                + ", tipoAccion=" + tipoAccion()
                + ", parametros=" + Arrays.toString(parametros) + "}";
    }
}
