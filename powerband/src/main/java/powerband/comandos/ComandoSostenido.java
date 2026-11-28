package powerband.comandos;

import java.util.Arrays;
import java.util.Objects;

import powerband.logging.Log;
import powerband.logging.LoggerPowerBand;
import powerband.logging.OrigenLog;
import powerband.sintetizadores.ParametrosNota;
import powerband.sintetizadores.Sintetizador;

public abstract class ComandoSostenido implements Comando {

    private final Sintetizador destino;
    private final String identificadorAccion;
    private final TipoAccion tipoAccion;
    private final double[] parametros;
    private final ParametrosNota parametrosNota;

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
        this.parametrosNota =
                ParametrosNota.desdeParametrosSostenidos(this.parametros);
    }

    protected ComandoSostenido(
            Sintetizador destino,
            TipoAccion tipoAccion,
            String identificadorAccion,
            ParametrosNota parametrosNota) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.tipoAccion = Objects.requireNonNull(tipoAccion, "tipoAccion");
        this.identificadorAccion =
                Objects.requireNonNull(identificadorAccion, "identificadorAccion");
        if (identificadorAccion.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de accion no puede estar vacio");
        }
        this.parametrosNota = Objects.requireNonNull(
                parametrosNota, "parametrosNota");
        this.parametros = parametrosNota.parametrosSostenidos();
    }

    @Override
    public final void ejecutar() {
        switch (tipoAccion) {
            case INICIO ->
                    destino.iniciar(identificadorAccion, parametrosNota);
            case ACTUALIZACION ->
                    destino.actualizar(identificadorAccion, parametrosNota);
            case FINALIZACION ->
                    destino.finalizar(identificadorAccion);
            default -> throw new IllegalArgumentException(
                    "ComandoSostenido requiere una accion sostenida");
        }
        LoggerPowerBand.getInstance().registrar(
                Log.comando(this, OrigenLog.COMANDOS));
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

    @Override
    public final ParametrosNota parametrosNota() {
        return parametrosNota;
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
