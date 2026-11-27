package powerband.controlador;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.BusComandos;
import powerband.comandos.FabricaComandosGrabados;
import powerband.comandos.TipoAccion;
import powerband.excepciones.FalloTransporteException;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.EsperaReproduccion;
import powerband.grabaciones.FinalizadorGrabacion;
import powerband.grabaciones.GrabacionAbierta;
import powerband.grabaciones.Grabador;
import powerband.grabaciones.Reproductor;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
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

class AsignacionesYGrabacionControladorTest {

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
    void aplicaAsignacionDeDispositivoActivaYRechazaConflictos()
            throws Exception {
        Fixture fixture = crearFixture();
        List<String> errores = new ArrayList<>();
        fixture.controlador().registrarObservadorEstado(
                observadorErrores(errores));

        assertTrue(fixture.controlador().asignarDispositivo(
                "control", "pad-a"));
        assertTrue(fixture.controlador().conectar());
        assertEquals("pad-a",
                fixture.entradaControl().dispositivoSeleccionado());

        int iniciosAntesDeReemplazo = fixture.entradaControl().inicios();
        assertTrue(fixture.controlador().asignarDispositivo(
                "control", "pad-b"));
        assertEquals("pad-b",
                fixture.entradaControl().dispositivoSeleccionado());
        assertTrue(fixture.entradaControl().estaActiva());
        assertEquals(iniciosAntesDeReemplazo + 1,
                fixture.entradaControl().inicios());

        assertFalse(fixture.controlador().asignarDispositivo(
                "microfono", "pad-b"));
        assertFalse(fixture.controlador().asignarDispositivo(
                "control", "no-disponible"));
        assertEquals(Map.of("control", "pad-b"),
                fixture.controlador().dispositivoPorInstrumento());
        assertEquals(2, errores.size());
        fixture.controlador().desconectar();
    }

    @Test
    void restauraLaSeleccionAnteriorSiFallaElReemplazo() throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        List<String> errores = new ArrayList<>();
        controlador.registrarObservadorEstado(observadorErrores(errores));
        assertTrue(controlador.asignarDispositivo("control", "pad-a"));
        assertTrue(controlador.conectar());
        fixture.entradaControl().fallarSeleccion("pad-b");

        assertFalse(controlador.asignarDispositivo("control", "pad-b"));

        assertEquals("pad-a", fixture.entradaControl().dispositivoSeleccionado());
        assertEquals(Map.of("control", "pad-a"), controlador.dispositivoPorInstrumento());
        assertTrue(fixture.entradaControl().estaActiva());
        assertEquals(1, errores.size());
        controlador.desconectar();
    }

    @Test
    void falloLocalAlActivarEntradaRevierteLaConexion()
            throws Exception {
        Fixture fixture = crearFixture();
        List<String> errores = new ArrayList<>();
        fixture.controlador().registrarObservadorEstado(
                observadorErrores(errores));
        fixture.entradaControl().fallarAlIniciar(true);

        assertFalse(fixture.controlador().conectar());

        assertEquals(EstadoControlador.INICIAL,
                fixture.controlador().estadoActual());
        assertFalse(fixture.entradaMicrofono().estaActiva());
        assertFalse(fixture.entradaControl().estaActiva());
        assertFalse(fixture.entradaTableta().estaActiva());
        assertEquals(1, errores.size());
    }

    @Test
    void desconexionIndividualConservaLasOtrasEntradasYLaGrabacion()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        List<String> errores = new ArrayList<>();
        controlador.registrarObservadorEstado(observadorErrores(errores));

        assertTrue(controlador.asignarDispositivo("microfono", "pad-a"));
        assertTrue(controlador.asignarDispositivo("control", "pad-b"));
        assertTrue(controlador.conectar());
        controlador.iniciarGrabacion();
        Path temporal = fixture.grabador().archivoSesionTemporal();

        fixture.entradaControl().desconectar("control desconectado");

        assertEquals(EstadoControlador.GRABANDO, controlador.estadoActual());
        assertFalse(fixture.entradaControl().estaActiva());
        assertNull(fixture.entradaControl().dispositivoSeleccionado());
        assertTrue(fixture.entradaMicrofono().estaActiva());
        assertTrue(fixture.entradaTableta().estaActiva());
        assertEquals(Map.of("microfono", "pad-a"),
                controlador.dispositivoPorInstrumento());
        assertEquals(1, errores.size());
        assertTrue(Files.exists(temporal));

        controlador.desconectar();
        assertFalse(Files.exists(temporal));
    }

    @Test
    void desconexionNotificaYRetiraAsignacionAunqueElLogNoSePuedaEscribir()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        List<String> errores = new ArrayList<>();
        List<String> retiros = new ArrayList<>();
        AtomicReference<Throwable> causaRecibida = new AtomicReference<>();
        controlador.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                throw new IllegalStateException("fallo observador de error");
            }

            @Override
            public void dispositivoDesconectado(String instrumento) {
                throw new IllegalStateException("fallo observador de retiro");
            }
        });
        controlador.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                errores.add(mensaje);
                causaRecibida.set(causa);
            }

            @Override
            public void dispositivoDesconectado(String instrumento) {
                retiros.add(instrumento);
            }
        });
        assertTrue(controlador.asignarDispositivo("control", "pad-a"));
        assertTrue(controlador.asignarDispositivo("microfono", "pad-b"));
        assertTrue(controlador.conectar());
        Path rutaInvalida = Files.createFile(carpetaTemporal.resolve("no-es-directorio"));
        LoggerPowerBand.cambiarPath(rutaInvalida);
        RuntimeException causa = new IllegalStateException("fallo de captura");
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        PrintStream anterior = System.err;
        try (PrintStream captura = new PrintStream(salida, true, StandardCharsets.UTF_8)) {
            System.setErr(captura);
            fixture.entradaControl().desconectar("control desconectado", causa);
            assertEquals(List.of("control"), retiros);
            assertEquals(1, errores.size());
            assertTrue(errores.get(0).contains("control desconectado"));
            assertSame(causa, causaRecibida.get());
            assertEquals(Map.of("microfono", "pad-b"), controlador.dispositivoPorInstrumento());
            assertFalse(fixture.entradaControl().estaActiva());
            assertTrue(fixture.entradaMicrofono().estaActiva());
        } finally {
            System.setErr(anterior);
            LoggerPowerBand.cambiarPath(carpetaTemporal);
            controlador.desconectar();
        }

        String salidaErrores = salida.toString(StandardCharsets.UTF_8);
        assertTrue(salidaErrores.contains("No se pudo escribir el log"));
        assertTrue(salidaErrores.contains("fallo observador de retiro"));
        assertTrue(salidaErrores.contains("fallo observador de error"));
    }

    @Test
    void retiroDuranteCierreGlobalEliminaLaAsignacionAfectada() throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        List<String> retirados = new ArrayList<>();
        controlador.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
            }

            @Override
            public void dispositivoDesconectado(String instrumento) {
                retirados.add(instrumento);
            }
        });
        assertTrue(controlador.asignarDispositivo("control", "pad-a"));
        assertTrue(controlador.conectar());
        fixture.entradaMicrofono().alDetener(
                () -> fixture.entradaControl().desconectar("retirado"));

        controlador.desconectar();

        assertTrue(controlador.dispositivoPorInstrumento().isEmpty());
        assertEquals(List.of("control"), retirados);
    }

    @Test
    void falloLocalDeTransporteDuranteGrabacionLimpiaElTemporal()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        List<Throwable> causas = new ArrayList<>();
        controlador.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                causas.add(causa);
            }
        });

        assertTrue(controlador.conectar());
        assertTrue(controlador.asignarSintetizador(
                "control", IdSintetizador.PIANO));
        controlador.iniciarGrabacion();
        fixture.control().convertirEvento(0.25, 0.75);
        Path temporal = fixture.grabador().archivoSesionTemporal();
        FalloTransporteException fallo = new FalloTransporteException(
                "fallo local de envío",
                new IOException("socket cerrado"));

        fixture.entradaControl().desconectar(fallo.getMessage(), fallo);

        assertEquals(EstadoControlador.INICIAL, controlador.estadoActual());
        assertFalse(fixture.grabador().estaGrabando());
        assertFalse(Files.exists(temporal));
        assertTrue(controlador.listarSesiones().isEmpty());
        assertEquals(1, causas.size());
        assertSame(fallo, causas.get(0));
    }

    @Test
    void cierreEsperaLaReproduccionYPermiteDesconectarYReconectar()
            throws Exception {
        CountDownLatch esperaIniciada = new CountDownLatch(1);
        CountDownLatch esperaFinalizada = new CountDownLatch(1);
        EsperaReproduccion espera = nanos -> {
            esperaIniciada.countDown();
            try {
                new CountDownLatch(1).await();
            } finally {
                esperaFinalizada.countDown();
            }
        };
        Fixture fixture = crearFixture(
                () -> System.nanoTime()
                        - TimeUnit.SECONDS.toNanos(20),
                espera);
        Controlador controlador = fixture.controlador();

        assertTrue(controlador.conectar());
        assertTrue(controlador.asignarSintetizador(
                "control", IdSintetizador.PIANO));
        controlador.iniciarGrabacion();
        fixture.control().convertirEvento(0.25, 0.75);
        controlador.detenerGrabacion("Sesion de cierre");
        assertTrue(controlador.reproducirSesion("Sesion de cierre"));
        assertTrue(esperaIniciada.await(2, TimeUnit.SECONDS));

        controlador.desconectar();

        assertTrue(esperaFinalizada.await(1, TimeUnit.SECONDS));
        assertEquals(EstadoControlador.INICIAL, controlador.estadoActual());
        int liberacionesTrasPrimerCierre =
                fixture.piano().liberaciones().get();
        controlador.desconectar();
        assertEquals(liberacionesTrasPrimerCierre,
                fixture.piano().liberaciones().get());

        assertTrue(controlador.conectar());
        controlador.desconectar();
        assertEquals(liberacionesTrasPrimerCierre + 1,
                fixture.piano().liberaciones().get());
    }

    @Test
    void falloDeTransporteDuranteElCierreNoBloqueaLaReproduccion()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        CountDownLatch reproduccionIniciada = new CountDownLatch(1);
        CountDownLatch cierreIniciado = new CountDownLatch(1);
        CountDownLatch cierreFinalizado = new CountDownLatch(1);
        AtomicReference<Throwable> falloCierre = new AtomicReference<>();
        List<String> errores = new java.util.concurrent.CopyOnWriteArrayList<>();
        controlador.registrarObservadorEstado(observadorErrores(errores));
        assertTrue(controlador.conectar());
        assertTrue(controlador.asignarSintetizador("control", IdSintetizador.PIANO));
        controlador.iniciarGrabacion();
        fixture.control().convertirEvento(0.25, 0.75);
        controlador.detenerGrabacion("Sesion de fallo");

        fixture.entradaMicrofono().alDetener(cierreIniciado::countDown);
        fixture.piano().alEjecutar(() -> {
            reproduccionIniciada.countDown();
            try {
                cierreIniciado.await();
            } catch (InterruptedException excepcion) {
                Thread.currentThread().interrupt();
            }
            throw new FalloTransporteException(
                    "fallo local", new IOException("transporte cerrado"));
        });
        assertTrue(controlador.reproducirSesion("Sesion de fallo"));
        assertTrue(reproduccionIniciada.await(2, TimeUnit.SECONDS));

        Thread cierre = new Thread(() -> {
            try {
                controlador.desconectar();
            } catch (RuntimeException excepcion) {
                falloCierre.set(excepcion);
            } finally {
                cierreFinalizado.countDown();
            }
        });
        cierre.setDaemon(true);
        cierre.start();

        assertTrue(cierreFinalizado.await(2, TimeUnit.SECONDS));
        assertNull(falloCierre.get());
        assertEquals(EstadoControlador.INICIAL, controlador.estadoActual());
        assertEquals(1, errores.size());
    }

    @Test
    void cierreContinuaConLosDemasRecursosTrasUnFallo()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        assertTrue(controlador.conectar());
        fixture.entradaControl().fallarAlDetener(true);
        fixture.piano().fallarLiberacion(true);

        RuntimeException fallo = assertThrows(
                IllegalStateException.class, controlador::desconectar);

        assertEquals(EstadoControlador.INICIAL, controlador.estadoActual());
        assertFalse(fixture.entradaMicrofono().estaActiva());
        assertFalse(fixture.entradaControl().estaActiva());
        assertFalse(fixture.entradaTableta().estaActiva());
        assertEquals(1, fixture.piano().liberaciones().get());
        assertTrue(fallo.getSuppressed().length > 0);
        SintetizadorFalso bajo = (SintetizadorFalso)
                fixture.sintetizadores().get(IdSintetizador.BAJO);
        assertEquals(1, bajo.liberaciones().get());
    }

    @Test
    void aplicaSintetizadoresEnVivoYConservaLaAsignacionAnteDatosInvalidos()
            throws Exception {
        Fixture fixture = crearFixture();
        assertTrue(fixture.controlador().conectar());
        List<String> errores = new ArrayList<>();
        fixture.controlador().registrarObservadorEstado(
                observadorErrores(errores));

        assertTrue(fixture.controlador().asignarSintetizador(
                "microfono", IdSintetizador.PIANO));
        assertEquals(List.of(fixture.piano()),
                fixture.microfono().sintetizadores());

        assertFalse(fixture.controlador().asignarSintetizador(
                "microfono", null));
        assertFalse(fixture.controlador().reemplazarSintetizadores(
                "microfono",
                Arrays.asList(IdSintetizador.BAJO, null)));

        assertEquals(List.of(IdSintetizador.PIANO),
                fixture.controlador().sintetizadoresPorInstrumento()
                        .get("microfono"));
        assertEquals(List.of(fixture.piano()),
                fixture.microfono().sintetizadores());
        assertEquals(2, errores.size());
        fixture.controlador().desconectar();
    }

    @Test
    void suscribeGrabadorYResuelveElSintetizadorAlReproducir()
            throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        AtomicBoolean reproduccionIniciada = new AtomicBoolean();
        CountDownLatch reproduccionFinalizada = new CountDownLatch(1);
        controlador.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                if (estado == EstadoControlador.REPRODUCIENDO) {
                    reproduccionIniciada.set(true);
                } else if (reproduccionIniciada.get()
                        && estado == EstadoControlador.CONECTADO) {
                    reproduccionFinalizada.countDown();
                }
            }

            @Override
            public void errorNotificado(
                    String mensaje, Throwable causa) {
            }
        });

        assertTrue(controlador.conectar());
        assertTrue(controlador.asignarSintetizador(
                "control", IdSintetizador.PIANO));
        controlador.iniciarGrabacion();
        fixture.control().convertirEvento(0.25, 0.75);
        SintetizadorFalso pianoActualizado =
                new SintetizadorFalso(IdSintetizador.PIANO);
        fixture.sintetizadores().put(
                IdSintetizador.PIANO, pianoActualizado);
        assertTrue(controlador.asignarSintetizador(
                "control", IdSintetizador.PIANO));
        fixture.control().convertirEvento(0.5, 0.75);
        controlador.detenerGrabacion("Sesion de prueba");

        try (GrabacionAbierta grabacion = fixture.cargador()
                .abrir("Sesion de prueba").orElseThrow()) {
            assertTrue(grabacion.siguienteComando().isPresent());
            assertTrue(grabacion.siguienteComando().isPresent());
            assertTrue(grabacion.siguienteComando().isEmpty());
        }

        assertTrue(controlador.reproducirSesion("Sesion de prueba"));
        assertTrue(reproduccionFinalizada.await(2, TimeUnit.SECONDS));
        assertEquals(1, fixture.piano().ejecuciones().get());
        assertEquals(3, pianoActualizado.ejecuciones().get());
        controlador.desconectar();
    }

    @Test
    void retiraElGrabadorDelBusAlFinalizar() throws Exception {
        Fixture fixture = crearFixture();
        Controlador controlador = fixture.controlador();
        assertTrue(controlador.conectar());
        assertTrue(controlador.asignarSintetizador("control", IdSintetizador.PIANO));

        controlador.iniciarGrabacion();
        fixture.control().convertirEvento(0.25, 0.75);
        controlador.detenerGrabacion("Sesion finalizada");
        fixture.control().convertirEvento(0.5, 0.75);

        assertEquals(List.of("Sesion finalizada"), controlador.buscarSesiones("finalizada"));
        try (GrabacionAbierta grabacion = fixture.cargador()
                .abrir("Sesion finalizada").orElseThrow()) {
            assertTrue(grabacion.siguienteComando().isPresent());
            assertTrue(grabacion.siguienteComando().isEmpty());
        }
        controlador.desconectar();
    }

    private Fixture crearFixture() throws Exception {
        return crearFixture(
                System::nanoTime,
                LockSupport::parkNanos);
    }

    private Fixture crearFixture(
            LongSupplier reloj, EsperaReproduccion espera)
            throws Exception {
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        EntradaMicrofonoFalsa entradaMicrofono =
                new EntradaMicrofonoFalsa(microfono);
        EntradaControlFalsa entradaControl =
                new EntradaControlFalsa(control);
        EntradaTabletaFalsa entradaTableta =
                new EntradaTabletaFalsa(tableta);

        Map<IdSintetizador, Sintetizador> sintetizadores =
                new ConcurrentHashMap<>();
        SintetizadorFalso piano =
                new SintetizadorFalso(IdSintetizador.PIANO);
        SintetizadorFalso bajo =
                new SintetizadorFalso(IdSintetizador.BAJO);
        sintetizadores.put(IdSintetizador.PIANO, piano);
        sintetizadores.put(IdSintetizador.BAJO, bajo);
        sintetizadores.put(
                IdSintetizador.ORGANO,
                new SintetizadorFalso(IdSintetizador.ORGANO));

        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(archivos.directorio(), reloj);
        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(grabador, archivos);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(sintetizadores::get);
        Reproductor reproductor = new Reproductor(
                fabrica::reconstruir,
                System::nanoTime,
                espera);

        Controlador controlador = new Controlador(
                bus,
                microfono,
                control,
                tableta,
                entradaMicrofono,
                entradaControl,
                entradaTableta,
                sintetizadores,
                grabador,
                cargador,
                finalizador,
                reproductor,
                archivos,
                () -> List.of("pad-a", "pad-b"));
        return new Fixture(
                controlador,
                microfono,
                control,
                entradaControl,
                entradaMicrofono,
                entradaTableta,
                grabador,
                cargador,
                piano,
                sintetizadores);
    }

    private static ObservadorEstado observadorErrores(
            List<String> errores) {
        return new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(
                    String mensaje, Throwable causa) {
                errores.add(mensaje);
            }
        };
    }

    private static final class EntradaControlFalsa
            extends EntradaControl {

        private boolean activa;
        private String dispositivoSeleccionado;
        private int inicios;
        private boolean fallarAlIniciar;
        private boolean fallarAlDetener;
        private String seleccionQueFalla;

        private EntradaControlFalsa(Control control) {
            super(control);
        }

        @Override
        public synchronized void iniciar() {
            activa = true;
            inicios++;
            if (fallarAlIniciar) {
                throw new IllegalStateException(
                        "fallo local al iniciar control");
            }
        }

        @Override
        public synchronized void detener() {
            activa = false;
            if (fallarAlDetener) {
                throw new IllegalStateException(
                        "fallo al detener control");
            }
        }

        @Override
        public synchronized boolean estaActiva() {
            return activa;
        }

        @Override
        public void seleccionarDispositivo(String nombre) {
            if (nombre.equals(seleccionQueFalla)) {
                throw new IllegalStateException("fallo al seleccionar control");
            }
            super.seleccionarDispositivo(nombre);
            dispositivoSeleccionado = nombre;
        }

        @Override
        public void limpiarDispositivoSeleccionado() {
            super.limpiarDispositivoSeleccionado();
            dispositivoSeleccionado = null;
        }

        private String dispositivoSeleccionado() {
            return dispositivoSeleccionado;
        }

        private int inicios() {
            return inicios;
        }

        private void fallarAlDetener(boolean fallar) {
            fallarAlDetener = fallar;
        }

        private void fallarAlIniciar(boolean fallar) {
            fallarAlIniciar = fallar;
        }

        private void fallarSeleccion(String nombre) {
            seleccionQueFalla = nombre;
        }
    }

    private static final class EntradaMicrofonoFalsa
            extends EntradaMicrofono {

        private boolean activa;
        private Runnable alDetener;

        private EntradaMicrofonoFalsa(Microfono microfono) {
            super(microfono);
        }

        @Override
        public synchronized void iniciar() {
            activa = true;
        }

        @Override
        public synchronized void detener() {
            activa = false;
            if (alDetener != null) {
                alDetener.run();
            }
        }

        @Override
        public synchronized boolean estaActiva() {
            return activa;
        }

        private void alDetener(Runnable accion) {
            alDetener = accion;
        }
    }

    private static final class EntradaTabletaFalsa
            extends EntradaTableta {

        private boolean activa;

        private EntradaTabletaFalsa(Tableta tableta) {
            super(tableta);
        }

        @Override
        public synchronized void iniciar() {
            activa = true;
        }

        @Override
        public synchronized void detener() {
            activa = false;
        }

        @Override
        public synchronized boolean estaActiva() {
            return activa;
        }
    }

    private static final class SintetizadorFalso
            implements Sintetizador {

        private final IdSintetizador id;
        private final AtomicInteger ejecuciones = new AtomicInteger();
        private final AtomicInteger liberaciones = new AtomicInteger();
        private boolean fallarLiberacion;
        private Runnable alEjecutar;

        private SintetizadorFalso(IdSintetizador id) {
            this.id = id;
        }

        @Override
        public IdSintetizador id() {
            return id;
        }

        @Override
        public void ejecutar(
                TipoAccion tipoAccion, double... parametros) {
            ejecuciones.incrementAndGet();
            if (alEjecutar != null) {
                alEjecutar.run();
            }
        }

        @Override
        public void liberarTodasLasNotas() {
            liberaciones.incrementAndGet();
            if (fallarLiberacion) {
                throw new IllegalStateException(
                        "fallo al liberar notas");
            }
        }

        private AtomicInteger ejecuciones() {
            return ejecuciones;
        }

        private AtomicInteger liberaciones() {
            return liberaciones;
        }

        private void fallarLiberacion(boolean fallar) {
            fallarLiberacion = fallar;
        }

        private void alEjecutar(Runnable accion) {
            alEjecutar = accion;
        }
    }

    private record Fixture(
            Controlador controlador,
            Microfono microfono,
            Control control,
            EntradaControlFalsa entradaControl,
            EntradaMicrofonoFalsa entradaMicrofono,
            EntradaTabletaFalsa entradaTableta,
            Grabador grabador,
            CargadorGrabaciones cargador,
            SintetizadorFalso piano,
            Map<IdSintetizador, Sintetizador> sintetizadores) {
    }
}
