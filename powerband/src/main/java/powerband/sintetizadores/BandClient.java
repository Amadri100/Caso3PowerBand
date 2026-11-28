package powerband.sintetizadores;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import powerband.util.Configuraciones;
import powerband.util.Constantes;

/**
 * OSC client that plays the synths defined in myband.sc
 * (classicPiano, stringBass, organ70s) on a running scsynth.
 *
 * It talks directly to scsynth using /s_new and /n_set, so no OSC library is
 * needed. The default destination comes from Configuraciones.
 */
public class BandClient implements AutoCloseable {

    private static final int ADD_TO_HEAD = 0;
    private static final int DEFAULT_GROUP = 1;
    private static final int FIRST_NODE_ID = 10000;
    private static final String PIANO = "piano";
    private static final String BAJO = "bajo";
    private static final String ORGANO = "organo";

    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;
    private final Configuraciones configuraciones;

    private int nextNodeId = FIRST_NODE_ID;
    private final Map<String, Map<Integer, Integer>> nodosActivos =
            new HashMap<>();

    public BandClient() throws IOException {
        this(Configuraciones.getInstance());
    }

    public BandClient(Configuraciones configuraciones) throws IOException {
        this(Objects.requireNonNull(configuraciones, "configuraciones").getHost(),
                configuraciones.getPuerto(),
                configuraciones);
    }

    public BandClient(String host, int port) throws IOException {
        this(host, port, Configuraciones.getInstance());
    }

    public BandClient(String host, int port,
                      Configuraciones configuraciones) throws IOException {
        Objects.requireNonNull(host, "host");
        if (host.isBlank()) {
            throw new IllegalArgumentException("El host no puede estar vacio");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
        }
        this.address = InetAddress.getByName(host);
        this.socket = new DatagramSocket();
        this.port = port;
        this.configuraciones =
                Objects.requireNonNull(configuraciones, "configuraciones");
        nodosActivos.put(PIANO, new HashMap<>());
        nodosActivos.put(BAJO, new HashMap<>());
        nodosActivos.put(ORGANO, new HashMap<>());
    }

    // ------------------------------------------------------------ synth API

    /** Classic piano chord (major) built on the given fundamental MIDI note. */
    public void playPiano(int note, int durMs) throws IOException {
        playPiano(note, durMs, true);
    }

    /** Classic piano chord, major or minor, built on the given fundamental. */
    public void playPiano(int note, int durMs, boolean major) throws IOException {
        validarNotaMidi(note);
        validarDuracion(durMs);
        sNew("classicPiano",
                "note", note,
                "durMs", durMs,
                "third", major ? 4 : 3,
                "sostenida", 0,
                "gate", 1,
                "amp", amplitudEfectiva(0.3f));
    }

    /** String bass note with a duration in milliseconds. */
    public void playBass(int note, int durMs) throws IOException {
        validarNotaMidi(note);
        validarDuracion(durMs);
        sNew("stringBass",
                "note", note,
                "durMs", durMs,
                "sostenida", 0,
                "gate", 1,
                "amp", amplitudEfectiva(0.5f));
    }

    public synchronized void startPiano(int note, boolean major)
            throws IOException {
        startPiano(note, major, 0.3f);
    }

    public synchronized void startPiano(
            int note, boolean major, float amplitude) throws IOException {
        validarNotaMidi(note);
        validarAmplitud(amplitude);
        stopPiano(note);
        validarCapacidad(PIANO, note);
        int id = sNew("classicPiano",
                "note", note,
                "durMs", Constantes.DURACION_MINIMA_MS,
                "third", major ? 4 : 3,
                "sostenida", 1,
                "gate", 1,
                "amp", amplitudEfectiva(amplitude));
        registrarNota(PIANO, note, id);
    }

    public synchronized void setPianoNote(int currentNote, int newNote)
            throws IOException {
        validarNotaMidi(currentNote);
        validarNotaMidi(newNote);
        Map<Integer, Integer> activos = nodosActivos.get(PIANO);
        Integer id = activos.get(currentNote);
        if (id == null) {
            return;
        }
        send("/n_set", id, "note", newNote);
        activos.remove(currentNote);
        activos.put(newNote, id);
    }

    public synchronized void setPianoThird(int note, int third)
            throws IOException {
        validarNotaMidi(note);
        if (third != Constantes.TERCERA_MENOR_SEMITONOS
                && third != Constantes.TERCERA_MAYOR_SEMITONOS) {
            throw new IllegalArgumentException(
                    "La tercera del piano debe ser menor (3) o mayor (4)");
        }
        Integer id = nodosActivos.get(PIANO).get(note);
        if (id != null) {
            send("/n_set", id, "third", third);
        }
    }

    public synchronized void setPianoAmplitude(int note, float amplitude)
            throws IOException {
        validarNotaMidi(note);
        validarAmplitud(amplitude);
        Integer id = nodosActivos.get(PIANO).get(note);
        if (id != null) {
            send("/n_set", id, "amp", amplitudEfectiva(amplitude));
        }
    }

    public synchronized void stopPiano(int note) throws IOException {
        detenerNodo(PIANO, note);
    }

    public synchronized void startBass(int note) throws IOException {
        startBass(note, 0.5f);
    }

    public synchronized void startBass(int note, float amplitude)
            throws IOException {
        validarNotaMidi(note);
        validarAmplitud(amplitude);
        stopBass(note);
        validarCapacidad(BAJO, note);
        int id = sNew("stringBass",
                "note", note,
                "durMs", Constantes.DURACION_MINIMA_MS,
                "sostenida", 1,
                "gate", 1,
                "amp", amplitudEfectiva(amplitude));
        registrarNota(BAJO, note, id);
    }

    public synchronized void setBassNote(int currentNote, int newNote)
            throws IOException {
        validarNotaMidi(currentNote);
        validarNotaMidi(newNote);
        Map<Integer, Integer> activos = nodosActivos.get(BAJO);
        Integer id = activos.get(currentNote);
        if (id == null) {
            return;
        }
        send("/n_set", id, "note", newNote);
        activos.remove(currentNote);
        activos.put(newNote, id);
    }

    public synchronized void setBassAmplitude(int note, float amplitude)
            throws IOException {
        validarNotaMidi(note);
        validarAmplitud(amplitude);
        Integer id = nodosActivos.get(BAJO).get(note);
        if (id != null) {
            send("/n_set", id, "amp", amplitudEfectiva(amplitude));
        }
    }

    public synchronized void stopBass(int note) throws IOException {
        detenerNodo(BAJO, note);
    }
    // Para poder "escuchar" la conexion el sistema manda un tono 
    // Si no se escucha el tono no hubo conexion
    public void playConnectionTone() throws IOException {
        for (int indice = 0;
                indice < Constantes.NOTAS_PIANO_TONO_CONEXION.size();
                indice++) {
            playPiano(
                    Constantes.NOTAS_PIANO_TONO_CONEXION.get(indice),
                    Constantes.DURACION_NOTA_TONO_CONEXION_MS);
            esperarNotaTonoConexion();

            playBass(
                    Constantes.NOTAS_BAJO_TONO_CONEXION.get(indice),
                    Constantes.DURACION_NOTA_TONO_CONEXION_MS);
            esperarNotaTonoConexion();

            int notaOrgano =
                    Constantes.NOTAS_ORGANO_TONO_CONEXION.get(indice);
            noteOn(notaOrgano);
            esperarNotaTonoConexion();
            noteOff(notaOrgano);
        }
    }

    /** 70's organ with default partials (1, 2, 3) and amplitudes. */
    public void noteOn(int note) throws IOException {
        noteOn(note,
                new float[] {
                        (float) Constantes.RAZON_ORGANO_1,
                        (float) Constantes.RAZON_ORGANO_2,
                        (float) Constantes.RAZON_ORGANO_3
                },
                new float[] {
                        (float) Constantes.AMPLITUD_ORGANO_1,
                        (float) Constantes.AMPLITUD_ORGANO_2,
                        (float) Constantes.AMPLITUD_ORGANO_3
                });
    }

    /** 70's organ with 3 partial ratios and 3 partial amplitudes. Sounds until noteOff. */
    public synchronized void noteOn(int note, float[] ratios, float[] amps) throws IOException {
        validarNotaMidi(note);
        if (ratios == null || amps == null
                || ratios.length != Constantes.CANTIDAD_PARCIALES_ORGANO
                || amps.length != Constantes.CANTIDAD_PARCIALES_ORGANO) {
            throw new IllegalArgumentException("organ needs exactly 3 ratios and 3 amps");
        }
        for (int indice = 0;
                indice < Constantes.CANTIDAD_PARCIALES_ORGANO;
                indice++) {
            if (!Float.isFinite(ratios[indice]) || ratios[indice] <= 0.0f) {
                throw new IllegalArgumentException(
                        "Las razones del organo deben ser positivas y finitas");
            }
            validarAmplitud(amps[indice]);
        }
        noteOff(note); // retrigger safely
        validarCapacidad(ORGANO, note);
        int id = sNew("organ70s",
                "freq", midiToFreq(note),
                "amp", amplitudEfectiva(0.3f),
                "r1", ratios[0], "r2", ratios[1], "r3", ratios[2],
                "a1", amplitudEfectiva(amps[0]),
                "a2", amplitudEfectiva(amps[1]),
                "a3", amplitudEfectiva(amps[2]));
        registrarNota(ORGANO, note, id);
    }

    public synchronized void noteOff(int note) throws IOException {
        detenerNodo(ORGANO, note);
    }

    public synchronized void setOrganNote(int currentNote, int newNote)
            throws IOException {
        validarNotaMidi(currentNote);
        validarNotaMidi(newNote);
        Map<Integer, Integer> activos = nodosActivos.get(ORGANO);
        Integer id = activos.get(currentNote);
        if (id == null) {
            return;
        }
        send("/n_set", id, "freq", midiToFreq(newNote));
        activos.remove(currentNote);
        activos.put(newNote, id);
    }

    public synchronized void setOrganAmplitude(int note, float amplitude)
            throws IOException {
        validarNotaMidi(note);
        validarAmplitud(amplitude);
        Integer id = nodosActivos.get(ORGANO).get(note);
        if (id != null) {
            send("/n_set", id, "amp", amplitudEfectiva(amplitude));
        }
    }

    public synchronized void setOrganPartials(
            int note, float[] ratios, float[] amps) throws IOException {
        validarNotaMidi(note);
        if (ratios == null || amps == null
                || ratios.length != Constantes.CANTIDAD_PARCIALES_ORGANO
                || amps.length != Constantes.CANTIDAD_PARCIALES_ORGANO) {
            throw new IllegalArgumentException(
                    "El organo necesita exactamente 3 razones y 3 amplitudes");
        }
        for (int indice = 0;
                indice < Constantes.CANTIDAD_PARCIALES_ORGANO;
                indice++) {
            if (!Float.isFinite(ratios[indice]) || ratios[indice] <= 0.0f) {
                throw new IllegalArgumentException(
                        "Las razones del organo deben ser positivas y finitas");
            }
            validarAmplitud(amps[indice]);
        }
        Integer id = nodosActivos.get(ORGANO).get(note);
        if (id != null) {
            send("/n_set", id,
                    "r1", ratios[0],
                    "r2", ratios[1],
                    "r3", ratios[2],
                    "a1", amplitudEfectiva(amps[0]),
                    "a2", amplitudEfectiva(amps[1]),
                    "a3", amplitudEfectiva(amps[2]));
        }
    }

    public synchronized void allNotesOff() throws IOException {
        clearAllSynths();
    }

    public synchronized void clearSynth(String synthName) throws IOException {
        String clave;
        if ("classicPiano".equals(synthName) || PIANO.equals(synthName)) {
            clave = PIANO;
        } else if ("stringBass".equals(synthName) || BAJO.equals(synthName)) {
            clave = BAJO;
        } else if ("organ70s".equals(synthName) || ORGANO.equals(synthName)) {
            clave = ORGANO;
        } else {
            throw new IllegalArgumentException(
                    "Sintetizador desconocido: " + synthName);
        }
        IOException falloEnvio = null;
        for (Integer note :
                new ArrayList<>(nodosActivos.get(clave).keySet())) {
            try {
                detenerNodo(clave, note);
            } catch (IOException fallo) {
                if (falloEnvio == null) {
                    falloEnvio = fallo;
                } else {
                    falloEnvio.addSuppressed(fallo);
                }
            }
        }
        if (falloEnvio != null) {
            throw falloEnvio;
        }
    }

    public synchronized void clearAllSynths() throws IOException {
        IOException falloEnvio = null;
        for (String synthName : new ArrayList<>(nodosActivos.keySet())) {
            try {
                clearSynth(synthName);
            } catch (IOException fallo) {
                if (falloEnvio == null) {
                    falloEnvio = fallo;
                } else {
                    falloEnvio.addSuppressed(fallo);
                }
            }
        }
        if (falloEnvio != null) {
            throw falloEnvio;
        }
    }

    // ------------------------------------------------------------- helpers

    public synchronized boolean isPianoActive(int note) {
        return nodosActivos.get(PIANO).containsKey(note);
    }

    public synchronized boolean isBassActive(int note) {
        return nodosActivos.get(BAJO).containsKey(note);
    }

    public synchronized boolean isOrganActive(int note) {
        return nodosActivos.get(ORGANO).containsKey(note);
    }

    private void registrarNota(String synthName, int note, int nodeId) {
        validarCapacidad(synthName, note);
        nodosActivos.get(synthName).put(note, nodeId);
    }

    private void validarCapacidad(String synthName, int note) {
        Map<Integer, Integer> activos = nodosActivos.get(synthName);
        if (activos.containsKey(note)) {
            return;
        }
        int total = nodosActivos.values().stream()
                .mapToInt(Map::size)
                .sum();
        if (total >= Constantes.MAXIMO_NODOS_SOSTENIDOS) {
            throw new IllegalStateException(
                    "Se alcanzó el máximo de nodos sostenidos");
        }
    }

    private synchronized void detenerNodo(String synthName, int note)
            throws IOException {
        Map<Integer, Integer> activos = nodosActivos.get(synthName);
        Integer id = activos.get(note);
        if (id != null) {
            send("/n_set", id, "gate", 0);
            activos.remove(note, id);
        }
    }

    private void validarNotaMidi(int note) {
        if (note < Constantes.NOTA_MIDI_MINIMA
                || note > Constantes.NOTA_MIDI_MAXIMA) {
            throw new IllegalArgumentException(
                    "La nota MIDI debe estar entre 0 y 127");
        }
    }

    private void validarDuracion(int duracionMs) {
        if (duracionMs < Constantes.DURACION_MINIMA_MS
                || duracionMs > Constantes.DURACION_MAXIMA_MS) {
            throw new IllegalArgumentException(
                    "La duración debe estar entre 1 milisegundo y "
                            + Constantes.DURACION_MAXIMA_MS);
        }
    }

    private void validarAmplitud(float amplitude) {
        if (!Float.isFinite(amplitude)
                || amplitude < (float) Constantes.AMPLITUD_MINIMA
                || amplitude > (float) Constantes.AMPLITUD_MAXIMA) {
            throw new IllegalArgumentException(
                    "La amplitud debe estar entre 0 y 1");
        }
    }

    private float amplitudEfectiva(float amplitude) {
        validarAmplitud(amplitude);
        Object valor = configuraciones.getOpcion(
                Constantes.OPCION_AMPLITUD_GENERAL);
        if (!(valor instanceof Number numeroGeneral)) {
            throw new IllegalStateException(
                    "La amplitud general no tiene un valor numérico");
        }
        float general = numeroGeneral.floatValue();
        if (!Float.isFinite(general)
                || general < (float) Constantes.AMPLITUD_MINIMA
                || general > (float) Constantes.AMPLITUD_MAXIMA) {
            throw new IllegalStateException(
                    "La amplitud general debe estar entre 0 y 1");
        }
        return amplitude * general;
    }

    private static float midiToFreq(int note) {
        return (float) (Constantes.FRECUENCIA_AFINACION_HZ
                * Math.pow(2.0,
                        (note - 69) / (double) Constantes.SEMITONOS_POR_OCTAVA));
    }

    /** Sends /s_new defName nodeId addAction target [name value ...]; returns the node id. */
    private synchronized int sNew(String defName, Object... controls) throws IOException {
        int id = nextNodeId++;
        Object[] args = new Object[4 + controls.length];
        args[0] = defName;
        args[1] = id;
        args[2] = ADD_TO_HEAD;
        args[3] = DEFAULT_GROUP;
        System.arraycopy(controls, 0, args, 4, controls.length);
        send("/s_new", args);
        return id;
    }

    /** Encodes and sends one OSC message. Supports Integer, Float and String arguments. */
    private void send(String path, Object... args) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, path);

        StringBuilder tags = new StringBuilder(",");
        for (Object a : args) {
            if (a instanceof Integer) tags.append('i');
            else if (a instanceof Float) tags.append('f');
            else if (a instanceof String) tags.append('s');
            else throw new IllegalArgumentException("unsupported OSC type: " + a.getClass());
        }
        writeString(out, tags.toString());

        for (Object a : args) {
            if (a instanceof Integer) writeInt(out, (Integer) a);
            else if (a instanceof Float) writeInt(out, Float.floatToIntBits((Float) a));
            else writeString(out, (String) a);
        }

        byte[] data = out.toByteArray();
        socket.send(new DatagramPacket(data, data.length, address, port));
    }

    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v >>> 24);
        out.write(v >>> 16);
        out.write(v >>> 8);
        out.write(v);
    }

    /** OSC string: bytes + null terminator, padded to a multiple of 4. */
    private static void writeString(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
        int pad = 4 - (b.length % 4); // always adds at least the terminating null
        for (int i = 0; i < pad; i++) out.write(0);
    }

    private static void esperarNotaTonoConexion() throws IOException {
        try {
            Thread.sleep(Constantes.DURACION_NOTA_TONO_CONEXION_MS);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Se interrumpió el tono de verificación", excepcion);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (socket.isClosed()) {
            return;
        }
        IOException falloEnvio = null;
        try {
            allNotesOff();
        } catch (IOException fallo) {
            falloEnvio = fallo;
        }
        socket.close();
        if (falloEnvio != null) {
            throw falloEnvio;
        }
    }

    // ---------------------------------------------------------------- test

    /**
     * Test: start SuperCollider, run ~myband.load; (server booted, synths added),
     * then run this main. Optional args: host port.
     */
    public static void main(String[] args) throws Exception {
        Configuraciones configuraciones = Configuraciones.getInstance();
        String host = args.length > 0 ? args[0] : configuraciones.getHost();
        int port = args.length > 1
                ? Integer.parseInt(args[1])
                : configuraciones.getPuerto();

        try (BandClient band = new BandClient(host, port)) {
            System.out.println("Connecting to scsynth at " + host + ":" + port);

            System.out.println("-- piano: C major, A minor, F major");
            band.playPiano(48, 1500);       Thread.sleep(1700);
            band.playPiano(57, 1500, false); Thread.sleep(1700);
            band.playPiano(53, 2000);       Thread.sleep(2300);

            System.out.println("-- string bass: C G A F");
            for (int n : new int[] { 36, 43, 45, 41 }) {
                band.playBass(n, 450);
                Thread.sleep(500);
            }
            Thread.sleep(500);

            System.out.println("-- organ 70s: noteOn / noteOff");
            band.noteOn(60);                                                    Thread.sleep(1500);
            band.noteOn(64, new float[] { 1f, 2.01f, 4f }, new float[] { 1f, 0.6f, 0.3f }); Thread.sleep(1500);
            band.noteOn(67, new float[] { 1f, 3f, 5f }, new float[] { 1f, 0.4f, 0.2f });    Thread.sleep(2000);
            band.noteOff(60);
            band.noteOff(64);
            System.out.println("-- organ 70s: allNotesOff");
            band.allNotesOff();
            Thread.sleep(1000);

            System.out.println("Done.");
        }
    }
}
