package powerband.comandos;

import powerband.sintetizadores.Sintetizador;

public interface Comando {

    void ejecutar();

    Sintetizador destino();

    TipoAccion tipoAccion();

    double[] parametros();
}
