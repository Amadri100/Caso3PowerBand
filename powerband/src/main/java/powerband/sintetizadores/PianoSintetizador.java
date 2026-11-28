package powerband.sintetizadores;

import java.util.Objects;

import powerband.util.Constantes;

public final class PianoSintetizador implements Sintetizador {

    private final BandClient bandClient;

    public PianoSintetizador(BandClient bandClient) {
        this.bandClient = Objects.requireNonNull(bandClient, "bandClient");
    }

    @Override
    public BandClient bandClient() {
        return bandClient;
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.PIANO;
    }

    @Override
    public void ejecutarPuntual(double... parametros) {
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
    public void ejecutarPuntual(ParametrosNota parametros) {
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.duracionMs() == null) {
            throw new IllegalArgumentException(
                    "Una ejecución puntual requiere duración");
        }
        enviar("enviar el acorde de piano",
                () -> bandClient.playPiano(
                        parametros.notaMidi(),
                        parametros.duracionMs(),
                        true));
    }

    @Override
    public void iniciar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        validarCantidad(parametros, 1, 2, "piano");
        int nota = notaMidi(parametros[0], "piano");
        boolean mayor = parametros.length == 2
                && parametros[1] == Constantes.TERCERA_MAYOR_SEMITONOS;
        enviar("iniciar el acorde sostenido de piano",
                () -> bandClient.startPiano(
                        voiceId, nota, mayor, 0.3f));
    }

    @Override
    public void iniciar(String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        enviar("iniciar el acorde sostenido de piano",
                () -> bandClient.startPiano(
                        voiceId,
                        parametros.notaMidi(),
                        true,
                        (float) parametros.amplitud()));
    }

    @Override
    public void actualizar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        validarCantidad(parametros, 1, 2, "piano");
        int nota = notaMidi(parametros[0], "piano");
        enviar("actualizar la nota de piano",
                () -> bandClient.setPianoNote(voiceId, nota));
        if (parametros.length == 2) {
            int tercera = enteroEnRango(parametros[1],
                    Constantes.TERCERA_MENOR_SEMITONOS,
                    Constantes.TERCERA_MAYOR_SEMITONOS,
                    "tercera", "piano");
            enviar("actualizar la tercera de piano",
                    () -> bandClient.setPianoThird(voiceId, tercera));
        }
    }

    @Override
    public void actualizar(String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        enviar("actualizar la nota de piano",
                () -> bandClient.setPianoNote(
                        voiceId, parametros.notaMidi()));
        enviar("actualizar la amplitud de piano",
                () -> bandClient.setPianoAmplitude(
                        voiceId, (float) parametros.amplitud()));
    }

    @Override
    public void finalizar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        enviar("finalizar la nota de piano",
                () -> bandClient.stopPiano(voiceId));
    }

    @Override
    public void liberarTodasLasNotas() {
        enviar("liberar todas las notas de piano",
                () -> bandClient.clearSynth("piano"));
    }

    private void validarIdentificadorVoz(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de voz no puede estar vacio");
        }
    }

}
