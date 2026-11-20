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

    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;

    private int nextNodeId = FIRST_NODE_ID;
    private final Map<Integer, Integer> activeOrgan = new HashMap<>(); // midi note -> node id

    public BandClient() throws IOException {
        this(Configuraciones.getInstance());
    }

    public BandClient(Configuraciones configuraciones) throws IOException {
        this(Objects.requireNonNull(configuraciones, "configuraciones").getHost(),
                configuraciones.getPuerto());
    }

    public BandClient(String host, int port) throws IOException {
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
    }

    // ------------------------------------------------------------ synth API

    /** Classic piano chord (major) built on the given fundamental MIDI note. */
    public void playPiano(int note, int durMs) throws IOException {
        playPiano(note, durMs, true);
    }

    /** Classic piano chord, major or minor, built on the given fundamental. */
    public void playPiano(int note, int durMs, boolean major) throws IOException {
        sNew("classicPiano",
                "note", note,
                "durMs", durMs,
                "third", major ? 4 : 3);
    }

    /** String bass note with a duration in milliseconds. */
    public void playBass(int note, int durMs) throws IOException {
        sNew("stringBass",
                "note", note,
                "durMs", durMs);
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
        if (ratios == null || amps == null
                || ratios.length != Constantes.CANTIDAD_PARCIALES_ORGANO
                || amps.length != Constantes.CANTIDAD_PARCIALES_ORGANO) {
            throw new IllegalArgumentException("organ needs exactly 3 ratios and 3 amps");
        }
        noteOff(note); // retrigger safely
        int id = sNew("organ70s",
                "freq", midiToFreq(note),
                "r1", ratios[0], "r2", ratios[1], "r3", ratios[2],
                "a1", amps[0], "a2", amps[1], "a3", amps[2]);
        activeOrgan.put(note, id);
    }

    public synchronized void noteOff(int note) throws IOException {
        Integer id = activeOrgan.get(note);
        if (id != null) {
            send("/n_set", id, "gate", 0);
            activeOrgan.remove(note, id);
        }
    }

    public synchronized void allNotesOff() throws IOException {
        IOException falloEnvio = null;
        for (Integer note : new ArrayList<>(activeOrgan.keySet())) {
            try {
                noteOff(note);
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
            band.noteOff(67);
            Thread.sleep(1000); // let the release tail finish

            System.out.println("Done.");
        }
    }
}
