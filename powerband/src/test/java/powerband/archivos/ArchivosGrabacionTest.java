package powerband.archivos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivosGrabacionTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void creaArchivosFaltantesEnCarpetaAparteYRegistraUbicaciones()
            throws IOException {
        Path carpetaDatos = carpetaTemporal.resolve("recordings");
        Path carpetaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(carpetaLogs);

        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaDatos);

        assertEquals(carpetaDatos.toAbsolutePath().normalize(),
                archivos.directorio());
        assertEquals(carpetaDatos.resolve("recordings.music")
                .toAbsolutePath().normalize(), archivos.archivoMusica());
        assertEquals(carpetaDatos.resolve("recordings.list")
                .toAbsolutePath().normalize(), archivos.archivoLista());
        assertEquals(carpetaDatos.resolve("recordings.index")
                .toAbsolutePath().normalize(), archivos.archivoIndice());
        assertTrue(Files.isRegularFile(archivos.archivoMusica()));
        assertTrue(Files.isRegularFile(archivos.archivoLista()));
        assertTrue(Files.isRegularFile(archivos.archivoIndice()));
        assertEquals(0, Files.size(archivos.archivoMusica()));
        assertEquals(0, Files.size(archivos.archivoLista()));
        assertEquals(0, Files.size(archivos.archivoIndice()));
        assertFalse(archivos.directorio().startsWith(carpetaLogs));

        String log = Files.readString(
                carpetaLogs.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(log.contains(archivos.archivoMusica().toString()));
        assertTrue(log.contains(archivos.archivoLista().toString()));
        assertTrue(log.contains(archivos.archivoIndice().toString()));
    }

    @Test
    void conservaDatosExistentesYNoRegistraComoNuevaSuCreacion()
            throws IOException {
        Path carpetaDatos = carpetaTemporal.resolve("recordings");
        Path archivoExistente = carpetaDatos.resolve("recordings.music");
        Files.createDirectories(carpetaDatos);
        byte[] datosExistentes = {1, 2, 3};
        Files.write(archivoExistente, datosExistentes);
        Path carpetaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(carpetaLogs);

        ArchivosGrabacion.inicializar(carpetaDatos);

        assertArrayEquals(datosExistentes, Files.readAllBytes(archivoExistente));
        String log = Files.readString(
                carpetaLogs.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertFalse(log.contains("recordings.music"));
        assertTrue(log.contains("recordings.list"));
        assertTrue(log.contains("recordings.index"));
    }

    @Test
    void rechazaQueUnaRutaDeArchivoOcupeElLugarDeLaCarpeta()
            throws IOException {
        Path rutaDirectorio = carpetaTemporal.resolve("no-es-directorio");
        Files.writeString(rutaDirectorio, "archivo existente");
        Path carpetaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(carpetaLogs);

        IOException excepcion = assertThrows(IOException.class,
                () -> ArchivosGrabacion.inicializar(rutaDirectorio));

        assertTrue(excepcion.getMessage().contains(
                rutaDirectorio.toAbsolutePath().normalize().toString()));
        String log = Files.readString(
                carpetaLogs.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(log.contains(rutaDirectorio.toString()));
    }
}
