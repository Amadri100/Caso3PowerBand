package powerband.logging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.excepciones.ConstantesExcepciones;
import powerband.util.Configuraciones;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerPowerBandTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarConfiguracion() {
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, false);
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void registraCategoriasYMensajeToString() throws Exception {

        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();

        logger.registrarComando("playPiano");
        logger.registrarInfo("conectado");
        logger.registrarError("servidor detenido");

        Path archivo = carpetaTemporal.resolve("powerband.log");
        String contenido = Files.readString(archivo, StandardCharsets.UTF_8);

        assertTrue(contenido.contains("[comando] playPiano"));
        assertTrue(contenido.contains("[info] conectado"));
        assertTrue(contenido.contains("[error] servidor detenido"));
    }

    @Test
    void incluyeUbicacionEnErroresDeArchivo() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();

        logger.registrarErrorArchivo("archivo invalido",
                "powerband/recordings/recordings.music");

        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(contenido.contains("archivo invalido"));
        assertTrue(contenido.contains("powerband/recordings/recordings.music"));
    }

    @Test
    void registraExcepcionNoCapturadaConDiagnostico() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, true);
        RuntimeException fallo = new RuntimeException("fallo inesperado");
        fallo.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("powerband.Demo", "ejecutar",
                        "Demo.java", 42)
        });

        LoggerPowerBand.getInstance().registrarExcepcionNoCapturada(
                fallo, OrigenLog.CONTROLADOR);

        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(contenido.contains(
                ConstantesExcepciones.MSG_EXCEPCION_NO_CAPTURADA));
        assertTrue(contenido.contains("RuntimeException: fallo inesperado"));
        assertTrue(contenido.contains("java.lang.RuntimeException: fallo inesperado"));
        assertTrue(contenido.contains(
                "ubicacionError=powerband.Demo.ejecutar(Demo.java:42)"));
        assertFalse(contenido.contains(" en ControladorDispositivos."));
    }

    @Test
    void rechazaObjetoVacio() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();

        assertThrows(IllegalArgumentException.class,
                () -> logger.registrarInfo(" "));
    }

    @Test
    void usaToStringDelObjeto() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        Object objeto = new Object() {
            @Override
            public String toString() {
                return "evento personalizado";
            }
        };

        logger.registrarInfo(objeto);

        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(contenido.contains("evento personalizado"));
        assertFalse(contenido.contains("Object"));
    }

    @Test
    void agregaDiagnosticoSoloCuandoEstaActivadoYConservaUnaLinea() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        IllegalStateException fallo = new IllegalStateException(
                "error\nprincipal", new IllegalArgumentException("causa\r\ninvalida"));
        fallo.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("powerband.Demo", "ejecutar", "Demo.java", 42)
        });
        fallo.addSuppressed(new UnsupportedOperationException("secundario"));
        Log evento = new Log(Instant.parse("2026-01-01T00:00:00Z"),
                CategoriaLog.ERROR, "lectura\nfallida", new DebugInfo(OrigenLog.ARCHIVOS, fallo));

        logger.registrar(evento);
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, true);
        fallo = new IllegalStateException("error\nprincipal", new IllegalArgumentException("causa\r\ninvalida"));
        fallo.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("powerband.Demo", "ejecutar", "Demo.java", 42)
        });
        fallo.addSuppressed(new UnsupportedOperationException("secundario"));
        Log segundo = new Log(Instant.parse("2026-01-02T00:00:00Z"),
                CategoriaLog.ERROR, "lectura\nfallida", new DebugInfo(OrigenLog.ARCHIVOS, fallo));
        logger.registrar(segundo);

        var lineas = Files.readAllLines(carpetaTemporal.resolve("powerband.log"), StandardCharsets.UTF_8);
        assertEquals(2, lineas.size());
        assertEquals("2026-01-01T00:00:00Z [error] lectura\\nfallida", lineas.get(0));
        String linea = lineas.get(1);
        assertTrue(linea.contains("origen=ARCHIVOS"));
        assertTrue(linea.contains("java.lang.IllegalStateException: error\\nprincipal"));
        assertTrue(linea.contains("java.lang.IllegalArgumentException: causa\\r\\ninvalida"));
        assertTrue(linea.contains("java.lang.UnsupportedOperationException: secundario"));
        assertTrue(linea.contains("powerband.Demo.ejecutar(Demo.java:42)"));
    }

    @Test
    void evitaDuplicadosAlPropagarUnFalloYaRegistrado() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        IllegalArgumentException fallo = new IllegalArgumentException("fallo");
        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        logger.registrar(Log.error("primer registro", OrigenLog.ARCHIVOS, fallo));
        logger.registrar(Log.error("propagado", OrigenLog.UI,
                new IllegalStateException("envoltorio", fallo)));
        assertEquals(1, Files.readAllLines(carpetaTemporal.resolve("powerband.log")).size());
    }

    @Test
    void noPierdeFallosSuprimidosAgregadosDespuesDelRegistro() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, true);
        IllegalStateException fallo = new IllegalStateException("principal");
        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        logger.registrar(Log.error("primer registro", OrigenLog.GRABACIONES, fallo));
        fallo.addSuppressed(new IllegalArgumentException("fallo de limpieza"));
        logger.registrar(Log.error("fallo al limpiar", OrigenLog.GRABACIONES, fallo));
        var lineas = Files.readAllLines(carpetaTemporal.resolve("powerband.log"));
        assertEquals(2, lineas.size());
        assertTrue(lineas.get(1).contains("fallo de limpieza"));
    }

    @Test
    void distingueFallosDiferentesAunqueSeanIguales() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand logger = LoggerPowerBand.getInstance();
        logger.registrar(Log.error("uno", OrigenLog.UI, new FalloIgual()));
        logger.registrar(Log.error("dos", OrigenLog.UI, new FalloIgual()));
        assertEquals(2, Files.readAllLines(carpetaTemporal.resolve("powerband.log")).size());
    }

    @Test
    void escapaSaltosDeLineaEnUbicacionesYAlFinalDelMensaje() throws Exception {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                "error\n", "ruta\r\narchivo", OrigenLog.ARCHIVOS, null));
        var lineas = Files.readAllLines(carpetaTemporal.resolve("powerband.log"));
        assertEquals(1, lineas.size());
        assertTrue(lineas.get(0).contains("error\\n ubicacion=ruta\\r\\narchivo"));
    }

    @Test
    void omiteThrowableAusenteYExponeUbicacionDesconocida() {
        Configuraciones.getInstance().setOpcion(Constantes.OPCION_DEBUG_LOG, true);
        String sinFallo = Log.info("evento", OrigenLog.CONTROLADOR).toString();
        assertFalse(sinFallo.contains("null"));
        assertFalse(sinFallo.contains("throwable="));
        IllegalStateException fallo = new IllegalStateException();
        fallo.setStackTrace(new StackTraceElement[0]);
        assertTrue(Log.error("error", OrigenLog.UI, fallo).toString()
                .contains("ubicacionError=desconocida"));
    }

    @Test
    void rechazaCamposObligatoriosInvalidosYFalloDeEscritura() throws Exception {
        assertThrows(NullPointerException.class,
                () -> new Log(null, CategoriaLog.INFO, "mensaje", null));
        assertThrows(NullPointerException.class,
                () -> new Log(Instant.now(), null, "mensaje", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Log(Instant.now(), CategoriaLog.INFO, " ", null));
        Path archivo = Files.createFile(carpetaTemporal.resolve("no-es-carpeta"));
        LoggerPowerBand.cambiarPath(archivo);
        assertThrows(IllegalStateException.class,
                () -> LoggerPowerBand.getInstance().registrar(Log.info("evento", OrigenLog.OTRO)));
    }

    private static final class FalloIgual extends RuntimeException {
        @Override
        public boolean equals(Object otro) {
            return otro instanceof FalloIgual;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
