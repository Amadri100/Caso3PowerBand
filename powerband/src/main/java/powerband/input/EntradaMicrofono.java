package powerband.input;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.sound.sampled.LineUnavailableException;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchProcessor;

import powerband.instrumentos.Microfono;
import powerband.util.Constantes;

public class EntradaMicrofono extends Entrada {

    private volatile AudioDispatcher dispatcher;
    private final Microfono microfono;
    private final BiConsumer<Double, Double> receptor;

    public EntradaMicrofono() {
        this((Microfono) null, null, null);
    }

    public EntradaMicrofono(Microfono microfono) {
        this(microfono, null, null);
    }

    public EntradaMicrofono(Microfono microfono,
                            Consumer<Entrada> callbackDesconexion) {
        this(microfono, null, callbackDesconexion);
    }

    public EntradaMicrofono(Microfono microfono,
                            Runnable callbackDesconexion) {
        super("microfono", callbackDesconexion);
        this.microfono = microfono;
        this.receptor = null;
    }

    public EntradaMicrofono(Microfono microfono,
                            BiConsumer<Double, Double> receptor,
                            Consumer<Entrada> callbackDesconexion) {
        super("microfono", callbackDesconexion);
        this.microfono = microfono;
        this.receptor = receptor;
    }

    public void procesarFrecuencia(double frecuenciaHz,
                                   double amplitud) {
        validarValor("frecuencia", frecuenciaHz,
                Constantes.FRECUENCIA_MINIMA_HZ,
                Constantes.FRECUENCIA_MAXIMA_HZ);
        validarValor("amplitud", amplitud,
                Constantes.AMPLITUD_MINIMA,
                Constantes.AMPLITUD_MAXIMA);
        if (receptor != null) {
            receptor.accept(frecuenciaHz, amplitud);
        } else if (microfono != null) {
            microfono.convertirFrecuencia(frecuenciaHz, amplitud);
        }
    }

    @Override
    protected void ejecutarCaptura() throws Exception {
        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
                    Constantes.TAMANO_BUFFER_AUDIO,
                    Constantes.SUPERPOSICION_AUDIO);
        } catch (LineUnavailableException excepcion) {
            throw new IllegalStateException(
                    "No hay un microfono disponible", excepcion);
        }

        PitchProcessor.PitchEstimationAlgorithm algoritmo =
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;
        dispatcher.addAudioProcessor(new PitchProcessor(
                algoritmo,
                44100.0f,
                Constantes.TAMANO_BUFFER_AUDIO,
                (resultado, evento) -> {
                    float frecuencia = resultado.getPitch();
                    if (frecuencia <= 0.0f || !resultado.isPitched()) {
                        return;
                    }
                    if (frecuencia > 0.0f) {
                        procesarFrecuencia(frecuencia,
                                Constantes.AMPLITUD_MAXIMA);
                    }
                }));
        dispatcher.run();
    }

    @Override
    protected void liberarRecursosEspecificos() {
        AudioDispatcher actual = dispatcher;
        if (actual != null) {
            actual.stop();
            dispatcher = null;
        }
    }
}
