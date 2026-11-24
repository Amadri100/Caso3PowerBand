package powerband.controlador;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.BusComandos;
import powerband.input.DisponibilidadDispositivo;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VigilanciaDispositivosTest {

    @TempDir
    Path temporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(temporal);
    }

    @AfterEach
    void restaurarLogger() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void retiraSoloLaAsignacionAusenteSinRestaurarlaAlReconectar() {
        Map<String, DisponibilidadDispositivo> estados = new ConcurrentHashMap<>();
        estados.put("microfono", DisponibilidadDispositivo.NO_DISPONIBLE);
        estados.put("control", DisponibilidadDispositivo.DISPONIBLE);
        Fixture fixture = crear((instrumento, id) -> estados.get(instrumento));
        assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));
        assertTrue(fixture.controlador().asignarDispositivo("control", "pad"));
        assertTrue(fixture.controlador().asignarDispositivo("tableta", "pen"));

        fixture.controlador().verificarDesconexiones();
        fixture.controlador().verificarDesconexiones();

        assertEquals(Map.of("control", "pad", "tableta", "pen"),
                fixture.controlador().dispositivoPorInstrumento());
        assertEquals(List.of("microfono"), fixture.desconexiones());
        estados.put("microfono", DisponibilidadDispositivo.DISPONIBLE);
        fixture.controlador().verificarDesconexiones();
        assertFalse(fixture.controlador().dispositivoPorInstrumento().containsKey("microfono"));
        assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));
    }

    @Test
    void noConfundeAmbiguedadNiErroresConUnaDesconexion() {
        AtomicReference<DisponibilidadDispositivo> estado =
                new AtomicReference<>(DisponibilidadDispositivo.AMBIGUO);
        Fixture fixture = crear((instrumento, id) -> estado.get());
        assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));

        fixture.controlador().verificarDesconexiones();
        fixture.controlador().verificarDesconexiones();
        assertEquals(1, fixture.errores().size());
        assertEquals("audio", fixture.controlador().dispositivoPorInstrumento().get("microfono"));
        assertTrue(fixture.desconexiones().isEmpty());

        estado.set(null);
        fixture.controlador().verificarDesconexiones();
        assertEquals(2, fixture.errores().size());
        assertEquals("audio", fixture.controlador().dispositivoPorInstrumento().get("microfono"));
    }

    @Test
    void descartaUnaComprobacionObsoletaTrasReasignarElMismoId() throws Exception {
        CountDownLatch consultaIniciada = new CountDownLatch(1);
        CountDownLatch liberarConsulta = new CountDownLatch(1);
        Fixture fixture = crear((instrumento, id) -> {
            consultaIniciada.countDown();
            try {
                if (!liberarConsulta.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Consulta no liberada");
                }
            } catch (InterruptedException fallo) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(fallo);
            }
            return DisponibilidadDispositivo.NO_DISPONIBLE;
        });
        assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));
        ExecutorService ejecutor = Executors.newSingleThreadExecutor();
        try {
            var consulta = ejecutor.submit(fixture.controlador()::verificarDesconexiones);
            assertTrue(consultaIniciada.await(5, TimeUnit.SECONDS));
            assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));
            liberarConsulta.countDown();
            consulta.get(5, TimeUnit.SECONDS);
            assertEquals("audio", fixture.controlador().dispositivoPorInstrumento().get("microfono"));
            assertTrue(fixture.desconexiones().isEmpty());
        } finally {
            liberarConsulta.countDown();
            ejecutor.shutdownNow();
        }
    }

    @Test
    void procesaElRetiroNotificadoPorJpenSoloParaLaTabletaAsignada() throws Exception {
        Fixture fixture = crear((instrumento, id) -> DisponibilidadDispositivo.DISPONIBLE);
        assertTrue(fixture.controlador().asignarDispositivo("tableta", "pen"));
        fixture.tableta().retirar("otro");
        fixture.tableta().retirar("pen");

        assertTrue(fixture.retiro().await(5, TimeUnit.SECONDS));
        assertTrue(fixture.controlador().dispositivoPorInstrumento().isEmpty());
        assertEquals(List.of("tableta"), fixture.desconexiones());
    }

    @Test
    void noDuplicaLaNotificacionSiLaCapturaTambienDetectaLaRetirada() {
        Fixture fixture = crear((instrumento, id) -> DisponibilidadDispositivo.NO_DISPONIBLE);
        assertTrue(fixture.controlador().asignarDispositivo("microfono", "audio"));
        fixture.microfono().alDetener(() -> fixture.microfono().desconectar("captura finalizada"));

        fixture.controlador().verificarDesconexiones();

        assertTrue(fixture.controlador().dispositivoPorInstrumento().isEmpty());
        assertEquals(List.of("microfono"), fixture.desconexiones());
    }

    private Fixture crear(BiFunction<String, String, DisponibilidadDispositivo> comprobador) {
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        TabletaSimulada entradaTableta = new TabletaSimulada(tableta);
        MicrofonoSimulado entradaMicrofono = new MicrofonoSimulado(microfono);
        List<String> errores = new CopyOnWriteArrayList<>();
        List<String> desconexiones = new CopyOnWriteArrayList<>();
        CountDownLatch retiro = new CountDownLatch(1);
        AtomicReference<ControladorDispositivos> referencia = new AtomicReference<>();
        Map<String, List<DispositivoEntrada>> catalogo = Map.of(
                "microfono", List.of(new DispositivoEntrada("audio", "Audio")),
                "control", List.of(new DispositivoEntrada("pad", "Control")),
                "tableta", List.of(new DispositivoEntrada("pen", "Lápiz")));
        ControladorDispositivos controlador = new ControladorDispositivos(
                microfono, control, tableta, entradaMicrofono,
                new EntradaControl(control), entradaTableta, new ConcurrentHashMap<>(),
                catalogo::get, comprobador, (mensaje, causa) -> errores.add(mensaje),
                (entrada, motivo, causa) -> {
                    referencia.get().retirarDispositivo(entrada.nombre());
                    desconexiones.add(entrada.nombre());
                    retiro.countDown();
                });
        referencia.set(controlador);
        return new Fixture(controlador, entradaMicrofono, entradaTableta, errores, desconexiones, retiro);
    }

    private record Fixture(ControladorDispositivos controlador, MicrofonoSimulado microfono,
            TabletaSimulada tableta,
            List<String> errores, List<String> desconexiones, CountDownLatch retiro) {
    }

    private static final class MicrofonoSimulado extends EntradaMicrofono {
        private Runnable alDetener;

        private MicrofonoSimulado(Microfono microfono) {
            super(microfono);
        }

        private void alDetener(Runnable accion) {
            alDetener = accion;
        }

        @Override
        public void detener() {
            super.detener();
            Runnable accion = alDetener;
            alDetener = null;
            if (accion != null) {
                accion.run();
            }
        }
    }

    private static final class TabletaSimulada extends EntradaTableta {
        private Consumer<String> observador;

        private TabletaSimulada(Tableta tableta) {
            super(tableta);
        }

        @Override
        public void registrarObservadorRetiro(Consumer<String> observador) {
            this.observador = observador;
        }

        private void retirar(String id) {
            observador.accept(id);
        }
    }
}
