package powerband.archivos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

class ReemplazadorIndiceTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void reemplazaSoloCuandoElResultadoEsValido() throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("golf", 60, 70),
                new EntradaIndice("alfa", 0, 10)));

        new OrdenadorIndice(rutaIndice).ordenarYReemplazar();

        assertEquals(List.of("alfa", "golf"), leerNombres(rutaIndice));
        assertEquals(List.of("recordings.index"),
                nombresEnCarpeta(rutaIndice.getParent()));
    }

    @Test
    void conservaElIndiceAnteResultadoNoOrdenadoDuplicadoOIncorrecto()
            throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("alfa", 0, 10),
                new EntradaIndice("golf", 60, 70)));
        byte[] indiceAnterior = Files.readAllBytes(rutaIndice);
        Path resultadoDuplicado = crearResultado(rutaIndice.getParent(), "duplicado.tmp",
                List.of(
                new EntradaIndice("Alpha", 0, 10),
                new EntradaIndice("ALPHA", 10, 20)));

        IOException duplicado = assertThrows(IOException.class,
                () -> new ReemplazadorIndice(
                        rutaIndice, resultadoDuplicado).reemplazar());

        assertTrue(duplicado.getMessage().contains("ORD-002"));
        assertArrayEquals(indiceAnterior, Files.readAllBytes(rutaIndice));
        assertTrue(Files.exists(resultadoDuplicado));

        Path resultadoCantidad = crearResultado(rutaIndice.getParent(), "cantidad.tmp",
                List.of(
                new EntradaIndice("solo", 0, 10)));
        assertThrows(IOException.class,
                () -> new ReemplazadorIndice(
                        rutaIndice, resultadoCantidad).reemplazar());
        assertArrayEquals(indiceAnterior, Files.readAllBytes(rutaIndice));
    }

    @Test
    void conservaElIndiceAnteResiduoEnElResultado() throws IOException {
        Path rutaIndice = crearIndice(List.of(
                new EntradaIndice("alfa", 0, 10)));
        byte[] indiceAnterior = Files.readAllBytes(rutaIndice);
        Path resultado = crearResultado(rutaIndice.getParent(), "residuo.tmp", List.of(
                new EntradaIndice("alfa", 0, 10)));
        Files.write(resultado, new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);

        assertThrows(IOException.class,
                () -> new ReemplazadorIndice(
                        rutaIndice, resultado).reemplazar());

        assertArrayEquals(indiceAnterior, Files.readAllBytes(rutaIndice));
    }

    private Path crearIndice(List<EntradaIndice> entradas)
            throws IOException {
        Path directorio = Files.createTempDirectory(
                carpetaTemporal, "indices-reemplazo-");
        LoggerPowerBand.cambiarPath(carpetaTemporal.resolve("logs"));
        Path ruta = directorio.resolve(
                Constantes.NOMBRE_ARCHIVO_GRABACIONES_INDICE);
        Files.createFile(ruta);
        escribir(ruta, entradas);
        return ruta;
    }

    private Path crearResultado(Path directorio, String nombre,
            List<EntradaIndice> entradas)
            throws IOException {
        Path ruta = directorio.resolve(nombre);
        Files.createFile(ruta);
        escribir(ruta, entradas);
        return ruta;
    }

    private void escribir(Path ruta, List<EntradaIndice> entradas)
            throws IOException {
        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoRegistros(ruta)) {
            long posicion = 0;
            for (EntradaIndice entrada : entradas) {
                archivo.escribir(posicion, entrada);
                posicion += Constantes.TAMANO_ENTRADA_INDICE;
            }
        }
    }

    private List<String> leerNombres(Path ruta) throws IOException {
        try (ArchivoRegistros<EntradaIndice> archivo =
                crearArchivoRegistros(ruta)) {
            long cantidad = archivo.cantidadRegistros();
            java.util.ArrayList<String> nombres = new java.util.ArrayList<>();
            for (long indice = 0; indice < cantidad; indice++) {
                nombres.add(archivo.leer(
                        indice * Constantes.TAMANO_ENTRADA_INDICE).nombre());
            }
            return List.copyOf(nombres);
        }
    }

    private List<String> nombresEnCarpeta(Path carpeta)
            throws IOException {
        try (var entradas = Files.list(carpeta)) {
            return entradas.map(ruta -> ruta.getFileName().toString())
                    .sorted().toList();
        }
    }

    private static ArchivoRegistros<EntradaIndice> crearArchivoRegistros(
            Path ruta) {
        return new ArchivoRegistros<>(ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }
}
