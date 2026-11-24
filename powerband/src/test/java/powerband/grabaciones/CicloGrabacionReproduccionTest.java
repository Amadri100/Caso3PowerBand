package powerband.grabaciones;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.archivos.BuscadorIndice;
import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ListaGrabaciones;
import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.FabricaComandosGrabados;
import powerband.comandos.TipoAccion;
import powerband.instrumentos.Microfono;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CicloGrabacionReproduccionTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @AfterEach
    void restaurarLogger() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void grabaBuscaYReproduceComandosEnOrdenYConSusIntervalos()
            throws IOException, InterruptedException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(
                        carpetaTemporal.resolve("grabaciones"));
        AtomicLong relojGrabacion =
                new AtomicLong(1_000_000_000L);
        Grabador grabador = new Grabador(
                archivos.directorio(), relojGrabacion::get);
        BusComandos bus = new BusComandos();
        AtomicInteger notificaciones = new AtomicInteger();
        bus.registrarObservador(grabador);
        bus.registrarObservador((comando, momento) -> {
            notificaciones.incrementAndGet();
            return true;
        });

        grabador.iniciar();
        long origenGrabacion = relojGrabacion.get();
        bus.publicar(comando(IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                60.0, 0.75), origenGrabacion + 10_000_000L);
        bus.publicar(comando(IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                67.0, 0.5), origenGrabacion + 35_000_000L);
        new FinalizadorGrabacion(
                grabador, archivos, () -> 1_700_000_000_000L)
                .finalizar("Sesion");

        assertEquals(List.of("Sesion"),
                new ListaGrabaciones(archivos.archivoLista())
                        .listar().stream()
                        .map(EntradaListaGrabacion::nombre).toList());
        assertTrue(new BuscadorIndice(archivos.archivoIndice())
                .buscar("sEsIoN").isPresent());

        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        try (GrabacionAbierta abierta =
                cargador.abrir("SESION").orElseThrow()) {
            assertEquals(10,
                    abierta.siguienteComando().orElseThrow()
                            .tiempoRelativoMillis());
            assertEquals(35,
                    abierta.siguienteComando().orElseThrow()
                            .tiempoRelativoMillis());
        }

        byte[] musicaAntes =
                Files.readAllBytes(archivos.archivoMusica());
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(id -> sintetizador);
        AtomicLong relojReproduccion = new AtomicLong();
        List<Long> esperas = new ArrayList<>();
        Reproductor reproductor = new Reproductor(
                fabrica::reconstruir,
                relojReproduccion::get,
                nanosegundos -> {
                    esperas.add(nanosegundos);
                    relojReproduccion.addAndGet(nanosegundos);
                });

        reproductor.reproducir(
                cargador.abrir("sesion").orElseThrow());

        assertEquals(List.of(TipoAccion.PUNTUAL, TipoAccion.PUNTUAL),
                sintetizador.accionesEjecutadas);
        assertArrayEquals(new double[] {60.0, 0.75},
                sintetizador.parametrosPorEjecucion.get(0));
        assertArrayEquals(new double[] {67.0, 0.5},
                sintetizador.parametrosPorEjecucion.get(1));
        assertEquals(List.of(10_000_000L, 25_000_000L), esperas);
        assertEquals(2, notificaciones.get());
        assertArrayEquals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica()));
    }

    @Test
    void integraInstrumentoConGrabacionBusquedaYReproduccion()
            throws IOException, InterruptedException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(
                        carpetaTemporal.resolve("integracion"));
        BusComandos bus = new BusComandos();
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        Microfono microfono = new Microfono(sintetizador, bus);
        Grabador grabador = new Grabador(archivos.directorio());
        bus.registrarObservador(grabador);

        grabador.iniciar();
        microfono.convertirFrecuencia(440.0, 0.4);
        microfono.convertirFrecuencia(880.0, 0.8);

        assertEquals(List.of(TipoAccion.PUNTUAL, TipoAccion.PUNTUAL),
                sintetizador.accionesEjecutadas);
        assertArrayEquals(new double[] {69.0, 300.0},
                sintetizador.parametrosPorEjecucion.get(0));
        assertArrayEquals(new double[] {81.0, 300.0},
                sintetizador.parametrosPorEjecucion.get(1));

        new FinalizadorGrabacion(
                grabador, archivos, () -> 1_700_000_000_000L)
                .finalizar("SesionIntegrada");

        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        assertEquals(List.of("SesionIntegrada"),
                cargador.listar().stream()
                        .map(EntradaListaGrabacion::nombre).toList());
        assertTrue(new BuscadorIndice(archivos.archivoIndice())
                .buscar("sEsIoNiNtEgRaDa").isPresent());

        sintetizador.accionesEjecutadas.clear();
        sintetizador.parametrosPorEjecucion.clear();
        AtomicLong relojReproduccion = new AtomicLong();
        Reproductor reproductor = new Reproductor(
                new FabricaComandosGrabados(id -> sintetizador)::reconstruir,
                relojReproduccion::get,
                relojReproduccion::addAndGet);

        reproductor.reproducir(
                cargador.abrir("SESIONINTEGRADA").orElseThrow());

        assertEquals(List.of(TipoAccion.PUNTUAL, TipoAccion.PUNTUAL),
                sintetizador.accionesEjecutadas);
        assertArrayEquals(new double[] {69.0, 300.0},
                sintetizador.parametrosPorEjecucion.get(0));
        assertArrayEquals(new double[] {81.0, 300.0},
                sintetizador.parametrosPorEjecucion.get(1));
    }

    private static Comando comando(IdSintetizador idSintetizador,
            TipoAccion tipoAccion, double... parametros) {
        Sintetizador destino =
                new SintetizadorPrueba(idSintetizador);
        double[] copiaParametros = parametros.clone();
        return new Comando() {
            @Override
            public void ejecutar() {
                destino.ejecutar(tipoAccion, parametros());
            }

            @Override
            public Sintetizador destino() {
                return destino;
            }

            @Override
            public TipoAccion tipoAccion() {
                return tipoAccion;
            }

            @Override
            public double[] parametros() {
                return copiaParametros.clone();
            }
        };
    }

    private static final class SintetizadorPrueba
            implements Sintetizador {

        private final IdSintetizador id;

        private SintetizadorPrueba(IdSintetizador id) {
            this.id = id;
        }

        @Override
        public IdSintetizador id() {
            return id;
        }

        @Override
        public void ejecutar(TipoAccion tipoAccion,
                double... parametros) {
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
