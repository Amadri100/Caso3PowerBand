package powerband.sintetizadores;

import powerband.comandos.TipoAccion;

public interface Sintetizador {

    IdSintetizador id();

    void ejecutar(TipoAccion tipoAccion, double... parametros);

    void liberarTodasLasNotas();
}
