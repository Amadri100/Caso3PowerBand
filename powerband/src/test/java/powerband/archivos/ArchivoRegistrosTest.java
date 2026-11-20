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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivoRegistrosTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void cuentaLeeReemplazaYAgregaRegistrosPorOffset() throws IOException {
        Path ruta = carpetaTemporal.resolve("recordings.index");
        Files.createFile(ruta);
        EntradaIndice primera = new EntradaIndice("primera", 0, 216);
        EntradaIndice segunda = new EntradaIndice("segunda", 216, 432);

        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoIndice(ruta)) {
            assertEquals(0, archivo.cantidadRegistros());
            archivo.escribir(0, primera);
            archivo.escribir(Constantes.TAMANO_ENTRADA_INDICE, segunda);
            assertEquals(2, archivo.cantidadRegistros());
            assertEquals(primera.nombre(), archivo.leer(0).nombre());
            assertEquals(segunda.nombre(),
                    archivo.leer(Constantes.TAMANO_ENTRADA_INDICE).nombre());

            EntradaIndice reemplazo =
                    new EntradaIndice("reemplazo", 10, 20);
            archivo.escribir(0, reemplazo);
            assertEquals("reemplazo", archivo.leer(0).nombre());
            assertEquals("segunda",
                    archivo.leer(Constantes.TAMANO_ENTRADA_INDICE).nombre());
            assertEquals(2, archivo.cantidadRegistros());
        }
    }

    @Test
    void rechazaResiduoYRegistraLaUbicacionDelArchivo() throws IOException {
        Path ruta = carpetaTemporal.resolve("incompleto.index");
        Files.write(ruta, new byte[Constantes.TAMANO_ENTRADA_INDICE + 1]);
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoIndice(ruta)) {
            IOException excepcion = assertThrows(IOException.class,
                    archivo::cantidadRegistros);
            assertTrue(excepcion.getMessage().contains(ruta.toString()));
            assertTrue(excepcion.getMessage().contains("residuo"));
            assertThrows(IOException.class, () -> archivo.leer(0));
            assertThrows(IOException.class,
                    () -> archivo.escribir(0,
                            new EntradaIndice("x", 0, 0)));
        }

        String log = Files.readString(
                carpetaTemporal.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(log.contains(ruta.toString()));
        assertTrue(log.contains("residuo"));
    }

    @Test
    void rechazaOffsetsInvalidosLecturasFueraDeRangoYGaps()
            throws IOException {
        Path ruta = carpetaTemporal.resolve("recordings.index");
        Files.createFile(ruta);
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        EntradaIndice entrada = new EntradaIndice("nombre", 0, 1);

        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoIndice(ruta)) {
            assertThrows(IllegalArgumentException.class,
                    () -> archivo.leer(-1));
            assertThrows(IllegalArgumentException.class,
                    () -> archivo.leer(1));
            assertThrows(IOException.class, () -> archivo.leer(0));
            assertThrows(IOException.class,
                    () -> archivo.escribir(216, entrada));
            assertEquals(0, Files.size(ruta));

            archivo.escribir(0, entrada);
            assertThrows(IOException.class, () -> archivo.leer(216));
        }
    }

    @Test
    void noCreaArchivoFaltanteYRechazaRegistroDeTamanoIncorrecto()
            throws IOException {
        Path rutaInexistente = carpetaTemporal.resolve("faltante.index");
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoIndice(rutaInexistente)) {
            assertThrows(IOException.class, archivo::cantidadRegistros);
        }
        assertFalse(Files.exists(rutaInexistente));

        try (ArchivoRegistros<byte[]> archivoBytes = new ArchivoRegistros<>(
                carpetaTemporal.resolve("bytes.data"), 2,
                datos -> datos, datos -> datos)) {
            Path rutaBytes = carpetaTemporal.resolve("bytes.data");
            Files.createFile(rutaBytes);
            assertThrows(IllegalArgumentException.class,
                    () -> archivoBytes.escribir(0, new byte[] {1}));
            assertEquals(0, Files.size(rutaBytes));
        }
    }

    @Test
    void rechazaLongitudesDeRegistroNoPositivas() {
        Path ruta = carpetaTemporal.resolve("recordings.index");
        assertThrows(IllegalArgumentException.class,
                () -> new ArchivoRegistros<>(ruta, 0,
                        EntradaIndice::desdeBytes, EntradaIndice::serializar));
    }

    @Test
    void cerrarLiberaElArchivoYRechazaNuevasOperaciones() throws IOException {
        Path ruta = carpetaTemporal.resolve("recordings.index");
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        Files.createFile(ruta);
        ArchivoRegistros<EntradaIndice> archivo = crearArchivoIndice(ruta);
        archivo.cantidadRegistros();

        archivo.close();

        assertThrows(IOException.class, archivo::cantidadRegistros);
        archivo.close();
        Files.delete(ruta);
    }

    private ArchivoRegistros<EntradaIndice> crearArchivoIndice(Path ruta) {
        return new ArchivoRegistros<>(ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
