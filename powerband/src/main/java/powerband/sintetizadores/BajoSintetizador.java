package powerband.sintetizadores;

import powerband.comandos.TipoAccion;

public final class BajoSintetizador extends AdaptadorBandClient {

    public BajoSintetizador(BandClient bandClient) {
        super(bandClient);
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.BAJO;
    }

    @Override
    public void ejecutar(TipoAccion tipoAccion, double... parametros) {
        validarAccion(tipoAccion, TipoAccion.PUNTUAL, "bajo");
        validarCantidad(parametros, 2, 2, "bajo");

        int nota = notaMidi(parametros[0], "bajo");
        int duracion = duracionMs(parametros[1], "bajo");
        enviar("enviar la nota de bajo",
                () -> bandClient.playBass(nota, duracion));
    }

    @Override
    public void liberarTodasLasNotas() {
    }
}
