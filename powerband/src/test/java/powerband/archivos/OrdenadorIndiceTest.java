package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenadorIndiceTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void ordenaEnVariasPasadasYConservaElIndiceOriginal()
            throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("golf", 60, 70),
                new EntradaIndice("foxtrot", 50, 60),
                new EntradaIndice("echo", 40, 50),
                new EntradaIndice("delta", 30, 40),
                new EntradaIndice("charlie", 20, 30),
                new EntradaIndice("bravo", 10, 20),
                new EntradaIndice("alfa", 0, 10)));
        byte[] indiceOriginal = Files.readAllBytes(rutaIndice);

        Path salida = new OrdenadorIndice(rutaIndice).ordenar();
        try {
            assertEquals(rutaIndice.getParent(), salida.getParent());
            assertEquals(indiceOriginal.length, Files.size(salida));
            assertArrayEquals(indiceOriginal, Files.readAllBytes(rutaIndice));
            assertEquals(List.of(
                            "alfa", "bravo", "charlie", "delta",
                            "echo", "foxtrot", "golf"),
                    leerNombres(salida));
            assertEquals(List.of("recordings.index",
                            salida.getFileName().toString()).stream()
                            .sorted().toList(),
                    nombresEnCarpeta(rutaIndice.getParent()));
        } finally {
            Files.deleteIfExists(salida);
        }
    }

    @Test
    void produceSalidaVaciaYOrdenaUnaEntrada() throws IOException {
        Path indiceVacio = crearIndice(List.of());
        Path salidaVacia = new OrdenadorIndice(indiceVacio).ordenar();
        Path salidaUnitaria = null;
        try {
            assertEquals(0, Files.size(salidaVacia));
            Path indiceUnitario = crearIndice(List.of(
                    new EntradaIndice("unica", 0, 1)));
            byte[] contenidoOriginal =
                    Files.readAllBytes(indiceUnitario);
            salidaUnitaria =
                    new OrdenadorIndice(indiceUnitario).ordenar();
            assertEquals(List.of("unica"), leerNombres(salidaUnitaria));
            assertArrayEquals(contenidoOriginal,
                    Files.readAllBytes(indiceUnitario));
        } finally {
            Files.deleteIfExists(salidaVacia);
            if (salidaUnitaria != null) {
                Files.deleteIfExists(salidaUnitaria);
            }
        }
    }

    @Test
    void rechazaDuplicadosYArchivosInvalidosSinAlterarElOrigen()
            throws IOException {
        Path indiceDuplicado = crearIndice(List.of(
                new EntradaIndice("Alpha", 0, 10),
                new EntradaIndice("ALPHA", 10, 20)));
        byte[] contenidoDuplicado = Files.readAllBytes(indiceDuplicado);

        assertThrows(IOException.class,
                () -> new OrdenadorIndice(indiceDuplicado).ordenar());
        assertArrayEquals(contenidoDuplicado,
                Files.readAllBytes(indiceDuplicado));
        assertEquals(List.of("recordings.index"),
                nombresEnCarpeta(indiceDuplicado.getParent()));

        Path indiceConResiduo = crearIndice(List.of(
                new EntradaIndice("nombre", 0, 1)));
        Files.write(indiceConResiduo, new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);
        byte[] contenidoConResiduo = Files.readAllBytes(indiceConResiduo);
        assertThrows(IOException.class,
                () -> new OrdenadorIndice(indiceConResiduo).ordenar());
        assertArrayEquals(contenidoConResiduo,
                Files.readAllBytes(indiceConResiduo));

        Path indiceMalFormado = crearIndice(List.of(
                new EntradaIndice("nombre", 0, 1)));
        byte[] datosMalFormados = Files.readAllBytes(indiceMalFormado);
        datosMalFormados[0] = (byte) 0xC3;
        Files.write(indiceMalFormado, datosMalFormados);
        byte[] contenidoMalFormado = Files.readAllBytes(indiceMalFormado);
        assertThrows(IOException.class,
                () -> new OrdenadorIndice(indiceMalFormado).ordenar());
        assertArrayEquals(contenidoMalFormado,
                Files.readAllBytes(indiceMalFormado));
    }

    @Test
    void rechazaNombresExactamenteDuplicadosDuranteLaMezcla()
            throws IOException {
        Path indice = crearIndice(List.of(
                new EntradaIndice("duplicado", 0, 10),
                new EntradaIndice("duplicado", 10, 20)));

        IOException excepcion = assertThrows(IOException.class,
                () -> new OrdenadorIndice(indice).ordenar());

        assertTrue(excepcion.getMessage().contains(
                "El índice contiene nombres duplicados: duplicado"));
        assertEquals(List.of("recordings.index"),
                nombresEnCarpeta(indice.getParent()));
    }

    private Path crearIndice(List<EntradaIndice> entradas)
            throws IOException {
        Path directorio = Files.createTempDirectory(
                carpetaTemporal, "indices-");
        Path rutaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(rutaLogs);
        Path rutaIndice = directorio.resolve("recordings.index");
        Files.createFile(rutaIndice);
        try (ArchivoRegistros<EntradaIndice> archivo =
                new ArchivoRegistros<>(
                rutaIndice, Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar)) {
            long posicion = 0;
            for (EntradaIndice entrada : entradas) {
                archivo.escribir(posicion, entrada);
                posicion += Constantes.TAMANO_ENTRADA_INDICE;
            }
        }
        return rutaIndice;
    }

    private List<String> leerNombres(Path rutaIndice) throws IOException {
        try (ArchivoRegistros<EntradaIndice> archivo = new ArchivoRegistros<>(
                rutaIndice, Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar)) {
            long cantidad = archivo.cantidadRegistros();
            List<String> nombres = new ArrayList<>();
            for (long indice = 0; indice < cantidad; indice++) {
                nombres.add(archivo.leer(
                        indice * Constantes.TAMANO_ENTRADA_INDICE).nombre());
            }
            return nombres;
        }
    }

    private List<String> nombresEnCarpeta(Path carpeta)
            throws IOException {
        try (var entradas = Files.list(carpeta)) {
            return entradas.map(ruta -> ruta.getFileName().toString())
                    .sorted().toList();
        }
    }
}
