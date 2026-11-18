package powerband.logging;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerPowerBandTest {

    @TempDir
    Path carpetaTemporal;

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
}
