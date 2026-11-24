package powerband.sintetizadores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.TipoAccion;
import powerband.excepciones.FalloTransporteException;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
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
    void losNueveParesInstrumentoSintetizadorUsanParametrosCompatibles() {
        ProgramadorNotasFalso programador = new ProgramadorNotasFalso();
        PianoSintetizador piano = new PianoSintetizador(cliente);
        BajoSintetizador bajo = new BajoSintetizador(cliente);
        OrganoSintetizador organo = new OrganoSintetizador(
                cliente, () -> programador);
        List<Sintetizador> sintetizadores = List.of(piano, bajo, organo);

        for (Sintetizador sintetizador : sintetizadores) {
            new Microfono(sintetizador).convertirFrecuencia(440.0, 0.5);
            new Control(sintetizador).convertirEvento(0.5, 0.5);
            new Tableta(sintetizador).convertirPosicion(0.8, 0.25, 0.7);
        }

        List<NotaPiano> notasEsperadas = List.of(
                new NotaPiano(69, 300, true),
                new NotaPiano(64, 500, true),
                new NotaPiano(102, 300, true));
        assertEquals(notasEsperadas, cliente.notasPiano);
        assertEquals(List.of(
                new NotaBajo(69, 300),
                new NotaBajo(64, 500),
                new NotaBajo(102, 300)), cliente.notasBajo);
        assertEquals(List.of(69, 64, 102), cliente.notasOrgano);
        assertEquals(List.of(300L, 500L, 300L), programador.demoras());

        programador.ejecutarTarea(0);
        programador.ejecutarTarea(1);
        programador.ejecutarTarea(2);
        assertEquals(List.of(69, 64, 102), cliente.notasFinalizadas);
        assertTrue(programador.cierres > 0);
    }

    @Test
    void vencimientoAnteriorNoFinalizaUnaNotaReiniciada() {
        ProgramadorNotasFalso programador = new ProgramadorNotasFalso();
        OrganoSintetizador organo = new OrganoSintetizador(
                cliente, () -> programador);

        organo.ejecutar(TipoAccion.PUNTUAL, 60, 10);
        organo.ejecutar(TipoAccion.PUNTUAL, 60, 20);
        assertEquals(List.of(60), cliente.notasFinalizadas);

        programador.ejecutarTarea(0);
        assertEquals(List.of(60), cliente.notasFinalizadas);

        programador.ejecutarTarea(1);
        assertEquals(List.of(60, 60), cliente.notasFinalizadas);
    }

    @Test
    void liberarTodasCancelaVencimientosPendientes() {
        ProgramadorNotasFalso programador = new ProgramadorNotasFalso();
        OrganoSintetizador organo = new OrganoSintetizador(
                cliente, () -> programador);

        organo.ejecutar(TipoAccion.PUNTUAL, 60, 50);
        organo.ejecutar(TipoAccion.PUNTUAL, 64, 50);
        organo.liberarTodasLasNotas();
        programador.ejecutarTodasLasTareas();

        assertEquals(Set.of(60, 64), Set.copyOf(cliente.notasFinalizadas));
        assertEquals(2, cliente.notasFinalizadas.size());
        assertTrue(programador.tareas.stream()
                .allMatch(tarea -> tarea.cancelada));
        assertEquals(1, programador.cierres);
    }

    @Test
    void siFallaLaProgramacionFinalizaLaNotaYPropagaElError() {
        ProgramadorNotasFalso programador = new ProgramadorNotasFalso();
        programador.fallarProgramacion = true;
        OrganoSintetizador organo = new OrganoSintetizador(
                cliente, () -> programador);

        assertThrows(IllegalStateException.class,
                () -> organo.ejecutar(TipoAccion.PUNTUAL, 60, 50));

        assertEquals(List.of(60), cliente.notasOrgano);
        assertEquals(List.of(60), cliente.notasFinalizadas);
        assertEquals(1, programador.cierres);
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

        assertTrue(error instanceof FalloTransporteException);
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

    private static final class ProgramadorNotasFalso
            implements OrganoSintetizador.ProgramadorNotas {

        private final List<TareaFalsa> tareas = new ArrayList<>();
        private boolean fallarProgramacion;
        private int cierres;

        @Override
        public OrganoSintetizador.TareaProgramada programar(
                Runnable tarea, long demoraMs) {
            if (fallarProgramacion) {
                throw new IllegalStateException("fallo programador");
            }
            tareas.add(new TareaFalsa(tarea, demoraMs));
            return tareas.get(tareas.size() - 1);
        }

        @Override
        public void cerrar() {
            cierres++;
        }

        private List<Long> demoras() {
            return tareas.stream().map(tarea -> tarea.demoraMs).toList();
        }

        private void ejecutarTarea(int indice) {
            tareas.get(indice).tarea.run();
        }

        private void ejecutarTodasLasTareas() {
            tareas.forEach(tarea -> tarea.tarea.run());
        }
    }

    private static final class TareaFalsa
            implements OrganoSintetizador.TareaProgramada {

        private final Runnable tarea;
        private final long demoraMs;
        private boolean cancelada;

        private TareaFalsa(Runnable tarea, long demoraMs) {
            this.tarea = tarea;
            this.demoraMs = demoraMs;
        }

        @Override
        public void cancelar() {
            cancelada = true;
        }
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
