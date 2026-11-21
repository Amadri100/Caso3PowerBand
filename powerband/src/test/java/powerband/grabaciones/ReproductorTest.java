package powerband.grabaciones;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.archivos.ComandoGrabado;
import powerband.archivos.EncabezadoGrabacion;
import powerband.archivos.EntradaIndice;
import powerband.comandos.FabricaComandosGrabados;
import powerband.comandos.TipoAccion;
import powerband.excepciones.ArchivoException;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproductorTest {

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
    void respetaPlazosDesdeElInicioSinReordenarComandosAtrasados()
            throws IOException, InterruptedException {
        ArchivosGrabacion archivos = crearGrabacion(
                comando(20, 1), comando(20, 2),
                comando(10, 3), comando(0, 4));
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        AtomicLong reloj = new AtomicLong(100);
        List<Long> esperas = new ArrayList<>();

        Reproductor reproductor = reproductor(sintetizador,
                reloj::get, nanosegundos -> {
                    esperas.add(nanosegundos);
                    assertTrue(nanosegundos > 0);
                    reloj.addAndGet(esperas.size() == 1
                            ? nanosegundos / 2 : nanosegundos);
                });
        reproductor.reproducir(abrir(archivos));

        assertEquals(List.of(1.0, 2.0, 3.0, 4.0),
                sintetizador.parametrosEjecutados);
        assertEquals(List.of(20_000_000L, 10_000_000L), esperas);
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void procesaSesionesMayoresQueElBufferSinPrecargarHastaElFinal()
            throws IOException {
        int capacidad = Constantes.CAPACIDAD_BUFFER_REPRODUCCION;
        assertTrue(capacidad > 0);
        byte[][] registros = new byte[capacidad * 2 + 1][];
        for (int indice = 0; indice < registros.length - 1; indice++) {
            registros[indice] = comando(0, indice).serializar();
        }
        registros[registros.length - 1] =
                new byte[Constantes.TAMANO_COMANDO_GRABADO];
        ArchivosGrabacion archivos = crearGrabacionDesdeBytes(registros);
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        Reproductor reproductor = reproductor(
                sintetizador, () -> 0,
                nanosegundos -> {
                    throw new AssertionError(
                            "No debe esperar para comandos con tiempo cero");
                });

        assertThrows(ArchivoException.class,
                () -> reproductor.reproducir(abrir(archivos)));
        assertEquals(capacidad * 2,
                sintetizador.parametrosEjecutados.size());
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void aceptaUnaSesionVaciaSinEsperar() throws IOException,
            InterruptedException {
        ArchivosGrabacion archivos = crearGrabacion();
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        AtomicInteger lecturasReloj = new AtomicInteger();
        Reproductor reproductor = reproductor(
                sintetizador,
                () -> {
                    lecturasReloj.incrementAndGet();
                    return 0;
                },
                nanosegundos -> {
                    throw new AssertionError(
                            "Una sesión vacía no debe esperar");
                });

        reproductor.reproducir(abrir(archivos));

        assertEquals(1, lecturasReloj.get());
        assertTrue(sintetizador.parametrosEjecutados.isEmpty());
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void propagaInterrupcionYLiberaLaSesion() throws IOException {
        ArchivosGrabacion archivos = crearGrabacion(comando(10, 1));
        InterruptedException interrupcion =
                new InterruptedException("espera interrumpida");
        Reproductor reproductor = reproductor(
                new SintetizadorFalso(IdSintetizador.PIANO),
                () -> 0,
                nanosegundos -> {
                    throw interrupcion;
                });

        assertSame(interrupcion, assertThrows(InterruptedException.class,
                () -> reproductor.reproducir(abrir(archivos))));
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void propagaFalloDeReconstruccionYEjecutaCierre() throws IOException {
        ArchivosGrabacion archivos = crearGrabacion(comando(0, 1));
        IllegalStateException fallo =
                new IllegalStateException("fallo al reconstruir");
        Reproductor reproductor = new Reproductor(
                grabado -> {
                    throw fallo;
                },
                () -> 0,
                nanosegundos -> {
                    throw new AssertionError(
                            "No debe esperar para un comando inmediato");
                });

        assertSame(fallo, assertThrows(IllegalStateException.class,
                () -> reproductor.reproducir(abrir(archivos))));
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void propagaFalloDeEjecucionYNoEjecutaComandosPosteriores()
            throws IOException {
        ArchivosGrabacion archivos = crearGrabacion(
                comando(0, 1), comando(0, 2));
        IllegalArgumentException fallo =
                new IllegalArgumentException("fallo al ejecutar");
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        sintetizador.falloEjecucion = fallo;
        Reproductor reproductor = reproductor(
                sintetizador, () -> 0,
                nanosegundos -> {
                    throw new AssertionError(
                            "No debe esperar para comandos inmediatos");
                });

        assertSame(fallo, assertThrows(IllegalArgumentException.class,
                () -> reproductor.reproducir(abrir(archivos))));
        assertTrue(sintetizador.parametrosEjecutados.isEmpty());
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void conservaFalloPrincipalSiFallaLaFinalizacionDeUnaNota()
            throws IOException {
        ArchivosGrabacion archivos = crearGrabacion(
                new ComandoGrabado(0, IdSintetizador.PIANO,
                        TipoAccion.INICIO, new double[] {60.0}),
                comando(0, 1));
        IllegalArgumentException falloEjecucion =
                new IllegalArgumentException("fallo puntual");
        IllegalStateException falloFinalizacion =
                new IllegalStateException("fallo al cerrar nota");
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        sintetizador.falloPuntual = falloEjecucion;
        sintetizador.falloFinalizacion = falloFinalizacion;
        Reproductor reproductor = reproductor(
                sintetizador, () -> 0,
                nanosegundos -> {
                    throw new AssertionError(
                            "No debe esperar para comandos inmediatos");
                });

        assertSame(falloEjecucion, assertThrows(
                IllegalArgumentException.class,
                () -> reproductor.reproducir(abrir(archivos))));

        assertEquals(1, falloEjecucion.getSuppressed().length);
        assertSame(falloFinalizacion,
                falloEjecucion.getSuppressed()[0]);
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void rechazaTiempoQueNoCabeEnNanosegundos() throws IOException {
        ArchivosGrabacion archivos = crearGrabacion(
                comando(Long.MAX_VALUE, 1));
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        Reproductor reproductor = reproductor(
                sintetizador, () -> 0,
                nanosegundos -> {
                    throw new AssertionError(
                            "No debe esperar un plazo no representable");
                });

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> reproductor.reproducir(abrir(archivos)));

        assertTrue(error.getCause() instanceof ArithmeticException);
        assertTrue(sintetizador.parametrosEjecutados.isEmpty());
        Files.delete(archivos.archivoMusica());
    }

    @Test
    void detenerInterrumpeLaEsperaYFinalizaSoloLasNotasActivas()
            throws IOException, InterruptedException {
        ArchivosGrabacion archivos = crearGrabacion(
                new ComandoGrabado(0, IdSintetizador.PIANO,
                        TipoAccion.INICIO, new double[] {60.0, 0.75}),
                new ComandoGrabado(0, IdSintetizador.PIANO,
                        TipoAccion.INICIO, new double[] {62.0, 0.5}),
                new ComandoGrabado(0, IdSintetizador.PIANO,
                        TipoAccion.FINALIZACION, new double[] {60.0}),
                comando(100, 99));
        SintetizadorFalso sintetizador =
                new SintetizadorFalso(IdSintetizador.PIANO);
        CountDownLatch inicioEspera = new CountDownLatch(1);
        CountDownLatch bloqueoEspera = new CountDownLatch(1);
        Reproductor reproductor = reproductor(
                sintetizador, () -> 0,
                nanosegundos -> {
                    inicioEspera.countDown();
                    bloqueoEspera.await();
                });
        AtomicReference<Throwable> fallo = new AtomicReference<>();
        Thread hilo = new Thread(() -> {
            try {
                reproductor.reproducir(abrir(archivos));
            } catch (Throwable excepcion) {
                fallo.set(excepcion);
            }
        });

        hilo.start();
        boolean detencionAceptada = false;
        try {
            assertTrue(inicioEspera.await(5, TimeUnit.SECONDS));
            detencionAceptada = reproductor.detener();
        } finally {
            reproductor.detener();
            hilo.join(TimeUnit.SECONDS.toMillis(5));
        }

        assertTrue(detencionAceptada);
        assertFalse(hilo.isAlive());
        assertNull(fallo.get());
        assertEquals(List.of(
                        TipoAccion.INICIO, TipoAccion.INICIO,
                        TipoAccion.FINALIZACION, TipoAccion.FINALIZACION),
                sintetizador.accionesEjecutadas);
        assertEquals(List.of(60.0, 62.0, 60.0, 62.0),
                sintetizador.parametrosEjecutados);
        assertFalse(reproductor.detener());
        Files.delete(archivos.archivoMusica());
    }

    private Reproductor reproductor(SintetizadorFalso sintetizador,
            LongSupplier reloj,
            EsperaReproduccion espera) {
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(id -> sintetizador);
        return new Reproductor(fabrica::reconstruir, reloj, espera);
    }

    private GrabacionAbierta abrir(ArchivosGrabacion archivos)
            throws IOException {
        return new CargadorGrabaciones(archivos)
                .abrir("sesion").orElseThrow();
    }

    private ArchivosGrabacion crearGrabacion(ComandoGrabado... comandos)
            throws IOException {
        byte[][] registros = new byte[comandos.length][];
        for (int indice = 0; indice < comandos.length; indice++) {
            registros[indice] = comandos[indice].serializar();
        }
        return crearGrabacionDesdeBytes(registros);
    }

    private ArchivosGrabacion crearGrabacionDesdeBytes(byte[]... registros)
            throws IOException {
        Path directorio = Files.createTempDirectory(
                carpetaTemporal, "grabacion-");
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(directorio);
        long posicionFinal = Constantes.TAMANO_ENCABEZADO_GRABACION
                + (long) registros.length
                        * Constantes.TAMANO_COMANDO_GRABADO;
        ByteArrayOutputStream musica = new ByteArrayOutputStream();
        musica.write(new EncabezadoGrabacion(
                "sesion", 0, posicionFinal, 100).serializar());
        for (byte[] registro : registros) {
            musica.write(registro);
        }
        Files.write(archivos.archivoMusica(), musica.toByteArray());
        Files.write(archivos.archivoIndice(),
                new EntradaIndice("sesion", 0, posicionFinal).serializar());
        return archivos;
    }

    private static ComandoGrabado comando(long tiempoMillis,
            double identificador) {
        return new ComandoGrabado(
                tiempoMillis, IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                new double[] {identificador});
    }
}
