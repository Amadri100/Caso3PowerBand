package powerband.sintetizadores;

import powerband.comandos.TipoAccion;
import powerband.util.Constantes;

public final class PianoSintetizador extends AdaptadorBandClient {

    public PianoSintetizador(BandClient bandClient) {
        super(bandClient);
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.PIANO;
    }

    @Override
    public void ejecutar(TipoAccion tipoAccion, double... parametros) {
        validarAccion(tipoAccion, TipoAccion.PUNTUAL, "piano");
        validarCantidad(parametros, 2, 3, "piano");

        int nota = notaMidi(parametros[0], "piano");
        int duracion = duracionMs(parametros[1], "piano");
        int tercera = parametros.length == 3
                ? enteroEnRango(parametros[2],
                        Constantes.TERCERA_MENOR_SEMITONOS,
                        Constantes.TERCERA_MAYOR_SEMITONOS,
                        "tercera", "piano")
                : Constantes.TERCERA_MAYOR_SEMITONOS;
        if (tercera != Constantes.TERCERA_MENOR_SEMITONOS
                && tercera != Constantes.TERCERA_MAYOR_SEMITONOS) {
            rechazar("La tercera del piano debe ser menor (3) o mayor (4)");
        }

        boolean mayor = tercera == Constantes.TERCERA_MAYOR_SEMITONOS;
        enviar("enviar el acorde de piano",
                () -> bandClient.playPiano(nota, duracion, mayor));
    }

    @Override
    public void liberarTodasLasNotas() {
    }
}
