package powerband.sintetizadores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptadoresSintetizadorTest {

    @TempDir
    Path carpetaTemporal;

    private ClienteBandPrueba cliente;

    @BeforeEach
    void preparar() throws IOException {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        cliente = new ClienteBandPrueba();
    }

    @AfterEach
    void cerrarCliente() throws IOException {
        cliente.close();
    }

    @Test
    void adaptadorDePianoAplicaTerceraMayorPorDefectoYPermiteMenor() {
        PianoSintetizador piano = new PianoSintetizador(cliente);

        piano.ejecutar(TipoAccion.PUNTUAL, 60, 750);
        piano.ejecutar(TipoAccion.PUNTUAL, 60, 750, 3);

        assertEquals(IdSintetizador.PIANO, piano.id());
        assertEquals(List.of(
                new NotaPiano(60, 750, true),
                new NotaPiano(60, 750, false)), cliente.notasPiano);
    }

    @Test
    void adaptadorDeBajoDelegaNotaYDuracion() {
        BajoSintetizador bajo = new BajoSintetizador(cliente);

        bajo.ejecutar(TipoAccion.PUNTUAL, 43, 900);

        assertEquals(IdSintetizador.BAJO, bajo.id());
        assertEquals(List.of(new NotaBajo(43, 900)), cliente.notasBajo);
    }

    @Test
    void adaptadorDeOrganoUsaValoresPredeterminadosOPersonalizados() {
        OrganoSintetizador organo = new OrganoSintetizador(cliente);

        organo.ejecutar(TipoAccion.INICIO, 60);
        organo.ejecutar(TipoAccion.INICIO,
                64, 1, 2.01, 4, 1, 0.6, 0.3);

        assertEquals(IdSintetizador.ORGANO, organo.id());
        assertEquals(List.of(60, 64), cliente.notasOrgano);
        assertArrayEquals(new float[] {1, 2, 3}, cliente.razonesOrgano.get(0));
        assertArrayEquals(new float[] {1, 0.5f, 0.25f},
                cliente.amplitudesOrgano.get(0));
        assertArrayEquals(new float[] {1, 2.01f, 4},
                cliente.razonesOrgano.get(1));
        assertArrayEquals(new float[] {1, 0.6f, 0.3f},
                cliente.amplitudesOrgano.get(1));
    }

    @Test
    void organoFinalizaNotasIndividualmenteYLiberaLasRestantes() {
        OrganoSintetizador organo = new OrganoSintetizador(cliente);
        organo.ejecutar(TipoAccion.INICIO, 60);
        organo.ejecutar(TipoAccion.INICIO, 64);

        organo.ejecutar(TipoAccion.FINALIZACION, 60);
        organo.liberarTodasLasNotas();
        organo.liberarTodasLasNotas();

        assertEquals(List.of(60, 64), cliente.notasFinalizadas);
    }

    @Test
    void rechazaYRegistraParametrosFueraDeRango() throws Exception {
        PianoSintetizador piano = new PianoSintetizador(cliente);
        BajoSintetizador bajo = new BajoSintetizador(cliente);
        OrganoSintetizador organo = new OrganoSintetizador(cliente);

        assertThrows(IllegalArgumentException.class,
                () -> piano.ejecutar(TipoAccion.PUNTUAL, -1, 100));
        assertThrows(IllegalArgumentException.class,
                () -> piano.ejecutar(TipoAccion.PUNTUAL, 60, 100, 2));
        assertThrows(IllegalArgumentException.class,
                () -> bajo.ejecutar(TipoAccion.PUNTUAL, 128, 100));
        assertThrows(IllegalArgumentException.class,
                () -> bajo.ejecutar(TipoAccion.PUNTUAL, 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> organo.ejecutar(TipoAccion.INICIO, 60,
                        0, 2, 3, 1, 0.5, 0.25));
        assertThrows(IllegalArgumentException.class,
                () -> organo.ejecutar(TipoAccion.INICIO, 60,
                        1, 2, 3, 1, 1.1, 0.25));
        assertThrows(IllegalArgumentException.class,
                () -> organo.ejecutar(TipoAccion.INICIO, 60,
                        Double.NaN, 2, 3, 1, 0.5, 0.25));

        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"));
        assertTrue(contenido.contains("fuera de rango"));
    }

    @Test
    void errorLocalDeUdpSeRegistraYSePropagaSinFormaDeExito()
            throws Exception {
        cliente.fallarEnvioPiano = true;
        PianoSintetizador piano = new PianoSintetizador(cliente);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> piano.ejecutar(TipoAccion.PUNTUAL, 60, 500));

        assertTrue(error.getCause() instanceof IOException);
        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"));
        assertTrue(contenido.contains("Fallo local al enviar el acorde de piano"));
    }

    @Test
    void organoConservaElRegistroDeNotaSiFallaSuCierre() {
        OrganoSintetizador organo = new OrganoSintetizador(cliente);
        organo.ejecutar(TipoAccion.INICIO, 60);
        cliente.fallarCierreOrgano = true;

        assertThrows(IllegalStateException.class,
                () -> organo.ejecutar(TipoAccion.FINALIZACION, 60));

        cliente.fallarCierreOrgano = false;
        organo.liberarTodasLasNotas();
        assertEquals(List.of(60, 60), cliente.notasFinalizadas);
    }

    private record NotaPiano(int nota, int duracion, boolean mayor) {
    }

    private record NotaBajo(int nota, int duracion) {
    }

    private static final class ClienteBandPrueba extends BandClient {

        private final List<NotaPiano> notasPiano = new ArrayList<>();
        private final List<NotaBajo> notasBajo = new ArrayList<>();
        private final List<Integer> notasOrgano = new ArrayList<>();
        private final List<Integer> notasFinalizadas = new ArrayList<>();
        private final List<float[]> razonesOrgano = new ArrayList<>();
        private final List<float[]> amplitudesOrgano = new ArrayList<>();
        private boolean fallarEnvioPiano;
        private boolean fallarCierreOrgano;

        private ClienteBandPrueba() throws IOException {
            super("127.0.0.1", 1);
        }

        @Override
        public void playPiano(int nota, int duracion, boolean mayor)
                throws IOException {
            if (fallarEnvioPiano) {
                throw new IOException("fallo piano");
            }
            notasPiano.add(new NotaPiano(nota, duracion, mayor));
        }

        @Override
        public void playBass(int nota, int duracion) {
            notasBajo.add(new NotaBajo(nota, duracion));
        }

        @Override
        public void noteOn(int nota, float[] razones, float[] amplitudes) {
            notasOrgano.add(nota);
            razonesOrgano.add(razones.clone());
            amplitudesOrgano.add(amplitudes.clone());
        }

        @Override
        public void noteOff(int nota) throws IOException {
            notasFinalizadas.add(nota);
            if (fallarCierreOrgano) {
                throw new IOException("fallo noteOff");
            }
        }
    }
}
