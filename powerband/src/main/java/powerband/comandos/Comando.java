package powerband.comandos;

import powerband.sintetizadores.Sintetizador;
import powerband.sintetizadores.ParametrosNota;

public interface Comando {

    void ejecutar();

    public Sintetizador destino();

    public TipoAccion tipoAccion();

    double[] parametros();

    public default ParametrosNota parametrosNota() {
        double[] datos = parametros();
        if (tipoAccion() == TipoAccion.PUNTUAL) {
            return ParametrosNota.desdeParametrosPuntuales(datos);
        }
        return ParametrosNota.desdeParametrosSostenidos(datos);
    }
}
