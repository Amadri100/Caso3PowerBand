package powerband.comandos;

import powerband.sintetizadores.Sintetizador;
import powerband.sintetizadores.ParametrosNota;

public final class ComandoInicio extends ComandoSostenido {

    public ComandoInicio(Sintetizador destino, String identificadorAccion,
                         double... parametros) {
        super(destino, TipoAccion.INICIO, identificadorAccion, parametros);
    }

    public ComandoInicio(Sintetizador destino, String identificadorAccion,
                         ParametrosNota parametrosNota) {
        super(destino, TipoAccion.INICIO,
                identificadorAccion, parametrosNota);
    }

    public ComandoInicio(Sintetizador destino, double... parametros) {
        this(destino, "inicio", parametros);
    }

    public ComandoInicio(Sintetizador destino, long identificadorAccion,
                         double... parametros) {
        this(destino, Long.toString(identificadorAccion), parametros);
    }

    public ComandoInicio(Sintetizador destino, TipoAccion tipoAccion,
                         String identificadorAccion, double... parametros) {
        this(destino, identificadorAccion, parametros);
        if (tipoAccion != TipoAccion.INICIO) {
            throw new IllegalArgumentException(
                    "ComandoInicio requiere la accion INICIO");
        }
    }
}
