package powerband.sintetizadores;

import java.util.Objects;

public final class BajoSintetizador implements Sintetizador {

    private final BandClient bandClient;

    public BajoSintetizador(BandClient bandClient) {
        this.bandClient = Objects.requireNonNull(bandClient, "bandClient");
    }

    @Override
    public BandClient bandClient() {
        return bandClient;
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.BAJO;
    }

    @Override
    public void ejecutarPuntual(double... parametros) {
        validarCantidad(parametros, 2, 2, "bajo");

        int nota = notaMidi(parametros[0], "bajo");
        int duracion = duracionMs(parametros[1], "bajo");
        enviar("enviar la nota de bajo",
                () -> bandClient.playBass(nota, duracion));
    }

    @Override
    public void ejecutarPuntual(ParametrosNota parametros) {
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.duracionMs() == null) {
            throw new IllegalArgumentException(
                    "Una ejecución puntual requiere duración");
        }
        enviar("enviar la nota de bajo",
                () -> bandClient.playBass(
                        parametros.notaMidi(),
                        parametros.duracionMs()));
    }

    @Override
    public void iniciar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        validarCantidad(parametros, 1, 1, "bajo");
        int nota = notaMidi(parametros[0], "bajo");
        enviar("iniciar la nota sostenida de bajo",
                () -> bandClient.startBass(voiceId, nota, 0.5f));
    }

    @Override
    public void iniciar(String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        enviar("iniciar la nota sostenida de bajo",
                () -> bandClient.startBass(
                        voiceId,
                        parametros.notaMidi(),
                        (float) parametros.amplitud()));
    }

    @Override
    public void actualizar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        validarCantidad(parametros, 1, 1, "bajo");
        int nota = notaMidi(parametros[0], "bajo");
        enviar("actualizar la nota de bajo",
                () -> bandClient.setBassNote(voiceId, nota));
    }

    @Override
    public void actualizar(String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        enviar("actualizar la nota de bajo",
                () -> bandClient.setBassNote(
                        voiceId, parametros.notaMidi()));
        enviar("actualizar la amplitud de bajo",
                () -> bandClient.setBassAmplitude(
                        voiceId, (float) parametros.amplitud()));
    }

    @Override
    public void finalizar(String voiceId, double... parametros) {
        validarIdentificadorVoz(voiceId);
        enviar("finalizar la nota de bajo",
                () -> bandClient.stopBass(voiceId));
    }

    @Override
    public void liberarTodasLasNotas() {
        enviar("liberar todas las notas de bajo",
                () -> bandClient.clearSynth("bajo"));
    }

    private void validarIdentificadorVoz(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de voz no puede estar vacio");
        }
    }

}
