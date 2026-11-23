package powerband.comandos;

import java.util.Arrays;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.Sintetizador;

public final class ComandoPuntual implements Comando {

    private final Sintetizador destino;
    private final double[] parametros;

    public ComandoPuntual(Sintetizador destino, double... parametros) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.parametros = parametros == null
                ? new double[0]
                : parametros.clone();
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
        destino.ejecutar(TipoAccion.PUNTUAL, parametros());
        LoggerPowerBand.getInstance().registrarComando(this);
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
    public String toString() {
        return "ComandoPuntual{destino=" + destino.id()
                + ", tipoAccion=" + tipoAccion()
                + ", parametros=" + Arrays.toString(parametros) + "}";
    }
}
