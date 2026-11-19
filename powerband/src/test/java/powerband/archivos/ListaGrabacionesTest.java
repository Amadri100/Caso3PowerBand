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

class ListaGrabacionesTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void agregaEnOrdenCronologicoYDesempataPorNombre() throws IOException {
        ArchivosGrabacion rutas = crearRutas();
        ListaGrabaciones lista = new ListaGrabaciones(rutas.archivoLista());

        lista.agregar(new EntradaListaGrabacion("delta", 200));
        lista.agregar(new EntradaListaGrabacion("charlie", 100));
        lista.agregar(new EntradaListaGrabacion("Alfa", 100));
        lista.agregar(new EntradaListaGrabacion("bravo", 100));
        lista.agregar(new EntradaListaGrabacion("zeta", 250));

        assertEquals(List.of("Alfa", "bravo", "charlie", "delta", "zeta"),
                lista.listar().stream()
                        .map(EntradaListaGrabacion::nombre).toList());
        assertEquals(List.of(100L, 100L, 100L, 200L, 250L),
                lista.listar().stream()
                        .map(EntradaListaGrabacion::fechaEpochMillis).toList());
        assertEquals(5L * Constantes.TAMANO_ENTRADA_LISTA_GRABACION,
                Files.size(rutas.archivoLista()));
    }

    @Test
    void listaVaciaNoModificaElArchivo() throws IOException {
        ArchivosGrabacion rutas = crearRutas();
        ListaGrabaciones lista = new ListaGrabaciones(rutas.archivoLista());
        byte[] contenidoAntes = Files.readAllBytes(rutas.archivoLista());

        assertEquals(List.of(), lista.listar());
        assertArrayEquals(contenidoAntes,
                Files.readAllBytes(rutas.archivoLista()));
    }

    @Test
    void rechazaDuplicadoPorComparacionSinCambiarLaLista()
            throws IOException {
        ArchivosGrabacion rutas = crearRutas();
        ListaGrabaciones lista = new ListaGrabaciones(rutas.archivoLista());
        lista.agregar(new EntradaListaGrabacion("sesion", 100));
        byte[] contenidoAntes = Files.readAllBytes(rutas.archivoLista());

        assertThrows(IllegalArgumentException.class,
                () -> lista.agregar(
                        new EntradaListaGrabacion("SESION", 50)));

        assertArrayEquals(contenidoAntes,
                Files.readAllBytes(rutas.archivoLista()));
        assertEquals(1, lista.listar().size());
    }

    @Test
    void residuoInvalidaListadoEInsercionSinAlterarElArchivo()
            throws IOException {
        ArchivosGrabacion rutas = crearRutas();
        ListaGrabaciones lista = new ListaGrabaciones(rutas.archivoLista());
        Files.write(rutas.archivoLista(), new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);
        byte[] contenidoAntes = Files.readAllBytes(rutas.archivoLista());

        assertThrows(IOException.class, lista::listar);
        assertThrows(IOException.class,
                () -> lista.agregar(new EntradaListaGrabacion("nueva", 0)));
        assertArrayEquals(contenidoAntes,
                Files.readAllBytes(rutas.archivoLista()));
    }

    private ArchivosGrabacion crearRutas() throws IOException {
        Path rutaLogs = carpetaTemporal.resolve("logs");
        LoggerPowerBand.cambiarPath(rutaLogs);
        return ArchivosGrabacion.inicializar(
                carpetaTemporal.resolve("recordings"));
    }
}
