package powerband.controlador;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FachadaControladorFalsaTest {

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
    void simulaTransicionesYEvitaNotificacionesDuplicadas() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        List<EstadoControlador> estados = new ArrayList<>();
        ObservadorEstado observador = new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                estados.add(estado);
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
            }
        };
        fachada.registrarObservadorEstado(observador);
        fachada.registrarObservadorEstado(observador);

        assertTrue(fachada.conectar());
        assertEquals(List.of(
                EstadoControlador.CONECTANDO,
                EstadoControlador.CONECTADO), estados);

        fachada.retirarObservadorEstado(observador);
        fachada.desconectar();
        assertEquals(2, estados.size());
        assertTrue(fachada.conectar());
    }

    @Test
    void entregaDatosDeEjemploEstablesYCopiasNoModificables() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();

        List<String> dispositivos = fachada.listarDispositivos();
        assertEquals(List.of(
                "audio-demo", "gamepad-demo", "lapiz-demo"),
                dispositivos);
        assertThrows(UnsupportedOperationException.class,
                () -> dispositivos.add("nuevo"));

        List<String> sesiones = fachada.listarSesiones();
        assertEquals(List.of("Demo Piano", "Demo Organo"), sesiones);
        assertThrows(UnsupportedOperationException.class,
                () -> sesiones.add("nueva"));
        assertEquals(List.of("Demo Organo"),
                fachada.buscarSesiones("demo organo"));
        assertTrue(fachada.buscarSesiones("ausente").isEmpty());
    }

    @Test
    void validaAsignacionesYNotificaEntradasInvalidas() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        List<String> errores = new ArrayList<>();
        fachada.registrarObservadorEstado(observadorErrores(errores));

        assertTrue(fachada.asignarDispositivo(
                "piano", "gamepad-demo"));
        assertFalse(fachada.asignarDispositivo(
                "instrumento-inexistente", "gamepad-demo"));
        assertTrue(fachada.asignarSintetizador(
                "organo", IdSintetizador.ORGANO));
        assertFalse(fachada.asignarSintetizador("organo", null));
        assertEquals(2, errores.size());
    }

    @Test
    void simulaGrabacionYRechazaNombresDuplicadosSinPerderSesiones() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        List<String> errores = new ArrayList<>();
        fachada.registrarObservadorEstado(observadorErrores(errores));
        fachada.conectar();
        fachada.iniciarGrabacion();
        fachada.detenerGrabacion("Demo Nueva");
        assertEquals(List.of(
                "Demo Piano", "Demo Organo", "Demo Nueva"),
                fachada.listarSesiones());

        fachada.iniciarGrabacion();
        assertThrows(IllegalArgumentException.class,
                () -> fachada.detenerGrabacion("dEmO nUeVa"));
        assertEquals(List.of(
                "Demo Piano", "Demo Organo", "Demo Nueva"),
                fachada.listarSesiones());
        assertEquals(1, errores.size());
        fachada.detenerGrabacion("Otra Demo");
    }

    @Test
    void simulaReproduccionYRegresaAlEstadoConectado() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        List<EstadoControlador> estados = new ArrayList<>();
        fachada.registrarObservadorEstado(observadorEstados(estados));
        fachada.conectar();

        assertTrue(fachada.reproducirSesion("demo piano"));
        assertFalse(fachada.reproducirSesion("no existe"));
        assertEquals(List.of(
                EstadoControlador.CONECTANDO,
                EstadoControlador.CONECTADO,
                EstadoControlador.REPRODUCIENDO,
                EstadoControlador.CONECTADO), estados);
    }

    @Test
    void rechazaOpcionesDesconocidasYDevuelveUnMapaInmodificable() {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        Map<String, Object> opciones = fachada.opcionesConfigurables();

        assertTrue(opciones.isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> opciones.put("host", "localhost"));
        assertThrows(IllegalArgumentException.class,
                () -> fachada.consultarOpcion("desconocida"));
        assertThrows(IllegalArgumentException.class,
                () -> fachada.establecerOpcion("desconocida", "valor"));
    }

    @Test
    void falloDeObservadorNoImpideNotificarALosDemas()
            throws IOException {
        FachadaControladorFalsa fachada =
                new FachadaControladorFalsa();
        int[] notificaciones = {0};
        fachada.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                throw new IllegalStateException("fallo de observador");
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                throw new IllegalStateException("fallo de observador");
            }
        });
        fachada.registrarObservadorEstado(observadorEstados(
                new ArrayList<>()));
        fachada.registrarObservadorEstado(new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                notificaciones[0]++;
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                notificaciones[0]++;
            }
        });

        fachada.conectar();
        assertFalse(fachada.asignarDispositivo("invalido", "no existe"));
        assertEquals(3, notificaciones[0]);
        assertTrue(Files.readString(
                carpetaTemporal.resolve("powerband.log"))
                .contains("Fallo al notificar"));
    }

    private static ObservadorEstado observadorErrores(List<String> errores) {
        return new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
                errores.add(mensaje);
            }
        };
    }

    private static ObservadorEstado observadorEstados(
            List<EstadoControlador> estados) {
        return new ObservadorEstado() {
            @Override
            public void estadoCambiado(EstadoControlador estado) {
                estados.add(estado);
            }

            @Override
            public void errorNotificado(String mensaje, Throwable causa) {
            }
        };
    }
}
