package powerband.comandos;

import java.util.Arrays;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.sintetizadores.Sintetizador;

public abstract class ComandoSostenido implements Comando {

    private final Sintetizador destino;
    private final String identificadorAccion;
    private final TipoAccion tipoAccion;
    private final double[] parametros;

    protected ComandoSostenido(
            Sintetizador destino,
            TipoAccion tipoAccion,
            String identificadorAccion,
            double... parametros) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.tipoAccion = Objects.requireNonNull(tipoAccion, "tipoAccion");
        this.identificadorAccion =
                Objects.requireNonNull(identificadorAccion, "identificadorAccion");
        if (identificadorAccion.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de accion no puede estar vacio");
        }
        this.parametros = parametros == null
                ? new double[0]
                : parametros.clone();
    }

    @Override
    public final void ejecutar() {
        destino.ejecutar(tipoAccion, parametros());
        LoggerPowerBand.getInstance().registrar(Log.comando(this, OrigenLog.COMANDOS));
    }

    @Override
    public final Sintetizador destino() {
        return destino;
    }

    @Override
    public final TipoAccion tipoAccion() {
        return tipoAccion;
    }

    @Override
    public final double[] parametros() {
        return parametros.clone();
    }

    public String identificadorAccion() {
        return identificadorAccion;
    }

    public String identificador() {
        return identificadorAccion;
    }

    protected String nombreTipo() {
        return getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return nombreTipo() + "{destino=" + destino.id()
                + ", tipoAccion=" + tipoAccion
                + ", identificadorAccion=" + identificadorAccion
                + ", parametros=" + Arrays.toString(parametros) + "}";
    }
}
