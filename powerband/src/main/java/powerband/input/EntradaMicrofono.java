package powerband.input;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;

import powerband.instrumentos.Microfono;
import powerband.util.Constantes;
import powerband.util.DispositivoEntrada;

public class EntradaMicrofono extends Entrada {

    private volatile AudioDispatcher dispatcher;
    private volatile TargetDataLine linea;
    private volatile String dispositivoSeleccionado;
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

    public static List<DispositivoEntrada> dispositivosCompatibles() {
        return dispositivosCompatibles(List.of(AudioSystem.getMixerInfo()),
                info -> AudioSystem.getMixer(info).isLineSupported(infoLinea()));
    }

    protected static List<DispositivoEntrada> dispositivosCompatibles(
            List<Mixer.Info> dispositivos, Predicate<Mixer.Info> compatible) {
        List<Mixer.Info> compatibles = dispositivos.stream()
                .filter(compatible)
                .toList();
        Map<String, Long> cantidades = compatibles.stream()
                .collect(Collectors.groupingBy(EntradaMicrofono::identificador,
                        Collectors.counting()));
        return compatibles.stream()
                .filter(info -> cantidades.get(identificador(info)) == 1)
                .map(info -> new DispositivoEntrada(identificador(info),
                        info.getName() + " (" + info.getVendor() + "; "
                                + info.getDescription() + "; " + info.getVersion() + ")"))
                .toList();
    }

    public void seleccionarDispositivo(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El identificador del microfono no puede estar vacio");
        }
        dispositivoSeleccionado = id;
    }

    public void limpiarDispositivoSeleccionado() {
        dispositivoSeleccionado = null;
    }

    private static AudioFormat formato() {
        return new AudioFormat(Constantes.FRECUENCIA_MUESTREO_AUDIO,
                Constantes.BITS_MUESTRA_AUDIO, Constantes.CANALES_AUDIO, true, false);
    }

    private static DataLine.Info infoLinea() {
        return new DataLine.Info(TargetDataLine.class, formato());
    }

    private static String identificador(Mixer.Info info) {
        String datos = info.getName() + "\u0000" + info.getVendor() + "\u0000"
                + info.getDescription() + "\u0000" + info.getVersion();
        return "microfono:" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(datos.getBytes(StandardCharsets.UTF_8));
    }

    public static DisponibilidadDispositivo comprobarDisponibilidad(String id) {
        return comprobarDisponibilidad(id, List.of(AudioSystem.getMixerInfo()),
                info -> AudioSystem.getMixer(info).isLineSupported(infoLinea()));
    }

    protected static DisponibilidadDispositivo comprobarDisponibilidad(
            String id, List<Mixer.Info> dispositivos, Predicate<Mixer.Info> compatible) {
        List<Mixer.Info> coincidencias = dispositivos.stream()
                .filter(info -> identificador(info).equals(id))
                .toList();
        if (coincidencias.size() > 1) {
            return DisponibilidadDispositivo.AMBIGUO;
        }
        return coincidencias.isEmpty() || !compatible.test(coincidencias.get(0))
                ? DisponibilidadDispositivo.NO_DISPONIBLE : DisponibilidadDispositivo.DISPONIBLE;
    }

    @Override
    protected void ejecutarCaptura() throws Exception {
        AudioDispatcher actual;
        synchronized (this) {
            if (!estaActiva()) {
                return;
            }
            try {
                String seleccionado = dispositivoSeleccionado;
                if (seleccionado == null) {
                    actual = AudioDispatcherFactory.fromDefaultMicrophone(
                            Constantes.TAMANO_BUFFER_AUDIO,
                            Constantes.SUPERPOSICION_AUDIO);
                } else {
                    Mixer.Info info = List.of(AudioSystem.getMixerInfo()).stream()
                            .filter(candidato -> identificador(candidato).equals(seleccionado))
                            .filter(candidato -> AudioSystem.getMixer(candidato)
                                    .isLineSupported(infoLinea()))
                            .reduce((primero, segundo) -> {
                                throw new IllegalStateException("El microfono seleccionado es ambiguo");
                            }).orElseThrow(() -> new IllegalStateException(
                                    "El microfono seleccionado ya no esta disponible"));
                    TargetDataLine nuevaLinea = (TargetDataLine)
                            AudioSystem.getMixer(info).getLine(infoLinea());
                    linea = nuevaLinea;
                    nuevaLinea.open(formato());
                    nuevaLinea.start();
                    actual = new AudioDispatcher(
                            new JVMAudioInputStream(new AudioInputStream(nuevaLinea)),
                            Constantes.TAMANO_BUFFER_AUDIO,
                            Constantes.SUPERPOSICION_AUDIO);
                }
                dispatcher = actual;
            } catch (LineUnavailableException | RuntimeException excepcion) {
                if (linea != null) {
                    linea.close();
                    linea = null;
                }
                throw new IllegalStateException(
                        "No se pudo abrir el microfono seleccionado", excepcion);
            }
        }

        PitchProcessor.PitchEstimationAlgorithm algoritmo =
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN;
        actual.addAudioProcessor(new PitchProcessor(
                algoritmo,
                Constantes.FRECUENCIA_MUESTREO_AUDIO,
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
        actual.run();
    }

    @Override
    protected synchronized void liberarRecursosEspecificos() {
        AudioDispatcher actual = dispatcher;
        if (actual != null) {
            actual.stop();
            dispatcher = null;
        }
        TargetDataLine lineaActual = linea;
        if (lineaActual != null) {
            lineaActual.close();
            linea = null;
        }
    }
}
