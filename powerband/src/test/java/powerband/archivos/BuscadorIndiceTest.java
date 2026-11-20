package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

class BuscadorIndiceTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void buscaEntradasSinDistinguirMayusculasYNoAlteraElIndice()
            throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("Alfa", 0, 10),
                new EntradaIndice("Bravo", 10, 20),
                new EntradaIndice("Charlie", 20, 30),
                new EntradaIndice("Delta", 30, 40),
                new EntradaIndice("Echo", 40, 50)));
        byte[] contenidoAntes = Files.readAllBytes(rutaIndice);
        BuscadorIndice buscador = new BuscadorIndice(rutaIndice);

        assertEquals(Optional.of("Alfa"),
                buscador.buscar("ALFA").map(EntradaIndice::nombre));
        assertEquals(Optional.of("Charlie"),
                buscador.buscar("cHaRlIe").map(EntradaIndice::nombre));
        assertEquals(Optional.of("Echo"),
                buscador.buscar("echo").map(EntradaIndice::nombre));
        assertEquals(Optional.empty(), buscador.buscar("foxtrot"));
        assertArrayEquals(contenidoAntes, Files.readAllBytes(rutaIndice));
    }

    @Test
    void indiceVacioDevuelveSinCoincidencia() throws IOException {
        Path rutaIndice = crearIndice(List.of());

        assertEquals(Optional.empty(),
                new BuscadorIndice(rutaIndice).buscar("nombre"));
    }

    @Test
    void rechazaConsultasInvalidasYArchivosConResiduo() throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("Alfa", 0, 10)));
        BuscadorIndice buscador = new BuscadorIndice(rutaIndice);

        assertThrows(IllegalArgumentException.class,
                () -> buscador.buscar(""));

        Files.write(rutaIndice, new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);
        assertThrows(IOException.class, () -> buscador.buscar("Alfa"));
    }

    @Test
    void indiceConUnRegistroBuscaCoincidenciaYAusencia() throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("unico", 40, 80)));
        BuscadorIndice buscador = new BuscadorIndice(rutaIndice);

        assertTrue(buscador.buscar("UNICO").isPresent());
        assertFalse(buscador.buscar("otro").isPresent());
    }

    @Test
    void buscaPrimeraIntermediaYUltimaEnCantidadPar() throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("Alfa", 0, 10),
                new EntradaIndice("Bravo", 10, 20),
                new EntradaIndice("Charlie", 20, 30),
                new EntradaIndice("Delta", 30, 40)));
        BuscadorIndice buscador = new BuscadorIndice(rutaIndice);

        assertTrue(buscador.buscar("alfa").isPresent());
        assertTrue(buscador.buscar("CHARLIE").isPresent());
        assertTrue(buscador.buscar("delta").isPresent());
    }

    private Path crearIndice(List<EntradaIndice> entradas)
            throws IOException {
        Path rutaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(rutaLogs);
        ArchivosGrabacion rutas = ArchivosGrabacion.inicializar(
                carpetaTemporal.resolve("recordings"));
        try (ArchivoRegistros<EntradaIndice> archivo =
                new ArchivoRegistros<>(
                rutas.archivoIndice(), Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar)) {
            long posicion = 0;
            for (EntradaIndice entrada : entradas) {
                archivo.escribir(posicion, entrada);
                posicion += Constantes.TAMANO_ENTRADA_INDICE;
            }
        }
        return rutas.archivoIndice();
    }
}
