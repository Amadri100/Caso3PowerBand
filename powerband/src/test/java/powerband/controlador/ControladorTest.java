package powerband.controlador;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.comandos.BusComandos;
import powerband.comandos.FabricaComandosGrabados;
import powerband.comandos.TipoAccion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.FinalizadorGrabacion;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControladorTest {

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
    void exponeUnaInstanciaUnicaDeProduccion() {
        assertSame(
                Controlador.getInstance(),
                Controlador.obtenerInstancia());
    }

    @Test
    void iniciaSinEntradasActivasYComparteLosServiciosInyectados()
            throws Exception {
        Controlador controlador = crearControlador();

        assertEquals(EstadoControlador.INICIAL,
                controlador.estadoActual());
        assertEquals(List.of(
                "microfono", "control", "tableta"),
                controlador.listarInstrumentos());
        assertEquals(List.of(
                IdSintetizador.PIANO,
                IdSintetizador.BAJO,
                IdSintetizador.ORGANO),
                controlador.listarSintetizadores());
    }

    @Test
    void asignaSintetizadoresSinExponerColeccionesMutables()
            throws Exception {
        Controlador controlador = crearControlador();

        assertEquals(true, controlador.reemplazarSintetizadores(
                "microfono",
                List.of(IdSintetizador.PIANO,
                        IdSintetizador.BAJO)));

        Map<String, List<IdSintetizador>> asignaciones =
                controlador.sintetizadoresPorInstrumento();
        assertEquals(List.of(
                IdSintetizador.PIANO,
                IdSintetizador.BAJO),
                asignaciones.get("microfono"));
        assertThrows(UnsupportedOperationException.class,
                () -> asignaciones.put(
                        "tableta", List.of(IdSintetizador.ORGANO)));

        Map<String, String> dispositivos =
                controlador.dispositivoPorInstrumento();
        assertThrows(UnsupportedOperationException.class,
                () -> dispositivos.put("microfono", "demo"));
    }

    @Test
    void registraObservadoresUnaSolaVezYRetiraUno()
            throws Exception {
        Controlador controlador = crearControlador();
        List<EstadoControlador> estados = new ArrayList<>();
        ObservadorEstado observador = new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                estados.add(estado);
            }

            @Override
            public void errorNotificado(
                    String mensaje, Throwable causa) {
            }
        };

        controlador.registrarObservadorEstado(observador);
        controlador.registrarObservadorEstado(observador);
        controlador.desconectar();
        assertEquals(0, estados.size());

        controlador.retirarObservadorEstado(observador);
        controlador.desconectar();
        assertEquals(0, estados.size());
    }

    private Controlador crearControlador() throws Exception {
        BusComandos busComandos = new BusComandos();
        Microfono microfono = new Microfono(null, busComandos);
        Control control = new Control(null, busComandos);
        Tableta tableta = new Tableta(null, busComandos);
        EntradaMicrofono entradaMicrofono =
                new EntradaMicrofono(microfono);
        EntradaControl entradaControl =
                new EntradaControl(control);
        EntradaTableta entradaTableta =
                new EntradaTableta(tableta);

        Map<IdSintetizador, Sintetizador> sintetizadores =
                new LinkedHashMap<>();
        sintetizadores.put(
                IdSintetizador.PIANO,
                new SintetizadorFalso(IdSintetizador.PIANO));
        sintetizadores.put(
                IdSintetizador.BAJO,
                new SintetizadorFalso(IdSintetizador.BAJO));
        sintetizadores.put(
                IdSintetizador.ORGANO,
                new SintetizadorFalso(IdSintetizador.ORGANO));

        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(archivos.directorio());
        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(grabador, archivos);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(
                        sintetizadores::get);
        Reproductor reproductor = new Reproductor(
                fabrica::reconstruir,
                System::nanoTime,
                nanos -> { });

        return new Controlador(
                busComandos,
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
                archivos);
    }

    private static final class SintetizadorFalso
            implements Sintetizador {

        private final IdSintetizador id;

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
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
