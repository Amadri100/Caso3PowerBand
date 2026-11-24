package powerband.instrumentos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.BusComandos;
import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrumentoTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @Test
    void reemplazaElSintetizadorConfigurado() {
        SintetizadorDePrueba primero = new SintetizadorDePrueba();
        SintetizadorDePrueba segundo = new SintetizadorDePrueba();
        InstrumentoDePrueba instrumento =
                new InstrumentoDePrueba(primero);

        assertSame(primero, instrumento.reemplazarSintetizador(segundo));
        assertSame(segundo, instrumento.sintetizador());
        assertEquals(List.of(segundo), instrumento.sintetizadores());

        instrumento.enviarAccion(TipoAccion.PUNTUAL, 60.0);
        assertEquals(TipoAccion.PUNTUAL, segundo.tipoAccion);
    }

    @Test
    void publicaUnaAccionPorCadaSintetizadorDestino() {
        SintetizadorDePrueba primero = new SintetizadorDePrueba();
        SintetizadorDePrueba segundo = new SintetizadorDePrueba();
        BusComandos bus = new BusComandos();
        List<Integer> publicaciones = new ArrayList<>();
        bus.registrarObservador((comando, momento) ->
                publicaciones.add(1));
        InstrumentoDePrueba instrumento =
                new InstrumentoDePrueba(primero, bus);
        instrumento.agregarSintetizador(segundo);

        instrumento.enviarAccion(TipoAccion.PUNTUAL, 60.0);

        assertEquals(2, publicaciones.size());
        assertEquals(TipoAccion.PUNTUAL, primero.tipoAccion);
        assertEquals(TipoAccion.PUNTUAL, segundo.tipoAccion);
    }

    @Test
    void dosInstrumentosPublicanAlMismoSintetizador() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        BusComandos bus = new BusComandos();
        List<Sintetizador> destinosPublicados = new ArrayList<>();
        bus.registrarObservador((comando, momento) ->
                destinosPublicados.add(comando.destino()));
        InstrumentoDePrueba primero =
                new InstrumentoDePrueba(sintetizador, bus);
        InstrumentoDePrueba segundo =
                new InstrumentoDePrueba(sintetizador, bus);

        primero.enviarAccion(TipoAccion.PUNTUAL, 60.0);
        segundo.enviarAccion(TipoAccion.INICIO, 64.0);

        assertEquals(List.of(sintetizador, sintetizador),
                destinosPublicados);
        assertEquals(2, sintetizador.ejecuciones);
        assertEquals(TipoAccion.INICIO, sintetizador.tipoAccion);
    }

    @Test
    void instrumentosConcretosConviertenSusDatos() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        Microfono microfono = new Microfono(sintetizador);
        Control control = new Control(sintetizador);
        Tableta tableta = new Tableta(sintetizador);

        microfono.convertirFrecuencia(440.0, 0.5);
        control.convertirEvento(0.25, 0.75);
        tableta.convertirPosicion(0.1, 0.2, 0.8);

        assertEquals(1, microfono.sintetizadores().size());
        assertEquals(1, control.sintetizadores().size());
        assertEquals(1, tableta.sintetizadores().size());
        assertEquals(69.0, microfono.frecuenciaAMidi(440.0));
        assertThrows(IllegalArgumentException.class,
                () -> microfono.frecuenciaAMidi(0.0));
    }

    @Test
    void convierteMicrofonoYTabletaAParametrosPuntualesComunes() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        Microfono microfono = new Microfono(sintetizador);
        Tableta tableta = new Tableta(sintetizador);

        microfono.convertirFrecuencia(440.0, 0.5);
        assertArrayEquals(new double[] {69.0, 300.0},
                sintetizador.parametros);

        tableta.convertirPosicion(0.8, 0.25, 0.7);
        assertArrayEquals(new double[] {102.0, 300.0},
                sintetizador.parametros);
    }

    @Test
    void rechazaEventosDeMicrofonoYTabletaFueraDeRango() {
        Microfono microfono =
                new Microfono(new SintetizadorDePrueba());
        Tableta tableta =
                new Tableta(new SintetizadorDePrueba());

        assertThrows(IllegalArgumentException.class,
                () -> microfono.convertirFrecuencia(20000.0, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> microfono.convertirFrecuencia(440.0, 1.1));
        assertThrows(IllegalArgumentException.class,
                () -> tableta.convertirPosicion(1.1, 0.5, 0.5));
    }

    @Test
    void convierteValoresNormalizadosDelControlAParametrosMusicales() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        Control control = new Control(sintetizador);

        control.convertirEvento(0.5, 0.499997118164);

        assertEquals(64.0, sintetizador.parametros[0]);
        assertEquals(500.0, sintetizador.parametros[1]);
    }

    @Test
    void mantieneParametrosDelControlEnRangoEnLosExtremos() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        Control control = new Control(sintetizador);

        control.convertirEvento(0.0, 0.0);
        assertEquals(0.0, sintetizador.parametros[0]);
        assertEquals(1.0, sintetizador.parametros[1]);

        control.convertirEvento(1.0, 1.0);
        assertEquals(127.0, sintetizador.parametros[0]);
        assertEquals(1000.0, sintetizador.parametros[1]);
    }

    @Test
    void rechazaValoresDelControlFueraDelRangoNormalizado() {
        Control control = new Control(new SintetizadorDePrueba());

        assertThrows(IllegalArgumentException.class,
                () -> control.convertirEvento(-0.1, 0.5));
        assertThrows(IllegalArgumentException.class,
                () -> control.convertirEvento(0.5, 1.1));
    }

    private static final class InstrumentoDePrueba extends Instrumento {

        private InstrumentoDePrueba(Sintetizador sintetizador) {
            super(sintetizador);
        }

        private InstrumentoDePrueba(Sintetizador sintetizador,
                                    BusComandos busComandos) {
            super(sintetizador, busComandos);
        }
    }

    private static final class SintetizadorDePrueba implements Sintetizador {

        private TipoAccion tipoAccion;
        private double[] parametros;
        private int ejecuciones;

        @Override
        public IdSintetizador id() {
            return IdSintetizador.PIANO;
        }

        @Override
        public void ejecutar(TipoAccion tipoAccion, double... parametros) {
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
            ejecuciones++;
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
