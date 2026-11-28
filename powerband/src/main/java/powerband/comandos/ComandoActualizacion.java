package powerband.comandos;

import powerband.sintetizadores.Sintetizador;
import powerband.sintetizadores.ParametrosNota;

public final class ComandoActualizacion extends ComandoSostenido {

    public ComandoActualizacion(Sintetizador destino,
                                String identificadorAccion,
                                double... parametros) {
        super(destino, TipoAccion.ACTUALIZACION,
                identificadorAccion, parametros);
    }

    public ComandoActualizacion(Sintetizador destino,
                                String identificadorAccion,
                                ParametrosNota parametrosNota) {
        super(destino, TipoAccion.ACTUALIZACION,
                identificadorAccion, parametrosNota);
    }

    public ComandoActualizacion(Sintetizador destino, double... parametros) {
        this(destino, "actualizacion", parametros);
    }

    public ComandoActualizacion(Sintetizador destino,
                                long identificadorAccion,
                                double... parametros) {
        this(destino, Long.toString(identificadorAccion), parametros);
    }

    public ComandoActualizacion(Sintetizador destino,
                                TipoAccion tipoAccion,
                                String identificadorAccion,
                                double... parametros) {
        this(destino, identificadorAccion, parametros);
        if (tipoAccion != TipoAccion.ACTUALIZACION) {
            throw new IllegalArgumentException(
                    "ComandoActualizacion requiere la accion ACTUALIZACION");
        }
    }
}
