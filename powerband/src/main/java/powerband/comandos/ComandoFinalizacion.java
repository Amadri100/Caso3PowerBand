package powerband.comandos;

import powerband.sintetizadores.Sintetizador;
import powerband.sintetizadores.ParametrosNota;

public final class ComandoFinalizacion extends ComandoSostenido {

    public ComandoFinalizacion(Sintetizador destino,
                               String identificadorAccion,
                               double... parametros) {
        super(destino, TipoAccion.FINALIZACION,
                identificadorAccion, parametros);
    }

    public ComandoFinalizacion(Sintetizador destino,
                               String identificadorAccion,
                               ParametrosNota parametrosNota) {
        super(destino, TipoAccion.FINALIZACION,
                identificadorAccion, parametrosNota);
    }

    public ComandoFinalizacion(Sintetizador destino, double... parametros) {
        this(destino, "finalizacion", parametros);
    }

    public ComandoFinalizacion(Sintetizador destino,
                               long identificadorAccion,
                               double... parametros) {
        this(destino, Long.toString(identificadorAccion), parametros);
    }

    public ComandoFinalizacion(Sintetizador destino,
                               TipoAccion tipoAccion,
                               String identificadorAccion,
                               double... parametros) {
        this(destino, identificadorAccion, parametros);
        if (tipoAccion != TipoAccion.FINALIZACION) {
            throw new IllegalArgumentException(
                    "ComandoFinalizacion requiere la accion FINALIZACION");
        }
    }
}
