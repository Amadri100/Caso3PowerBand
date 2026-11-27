package powerband.comandos;

import powerband.sintetizadores.Sintetizador;

public interface Comando {

    void ejecutar();

    public Sintetizador destino();

    public TipoAccion tipoAccion();

    double[] parametros();
}
