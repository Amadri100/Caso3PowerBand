package powerband.sintetizadores;

import powerband.comandos.TipoAccion;

public interface Sintetizador {

    public IdSintetizador id();

    public void ejecutar(TipoAccion tipoAccion, double... parametros);

    public void liberarTodasLasNotas();
}
