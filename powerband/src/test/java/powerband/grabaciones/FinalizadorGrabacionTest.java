package powerband.grabaciones;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.archivos.ArchivoRegistros;
import powerband.archivos.BuscadorIndice;
import powerband.archivos.EncabezadoGrabacion;
import powerband.archivos.EntradaIndice;
import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ListaGrabaciones;
import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.TipoAccion;
import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.NombreDuplicadoException;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinalizadorGrabacionTest {

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
    void finalizaYPersisteEncabezadoComandosListaEIndice()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        grabador.comandoEnviado(
                new ComandoPrueba(TipoAccion.PUNTUAL,
                        new double[] {60.0}),
                11_500_000L);

        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(
                        grabador, archivos, () -> 1234L);
        finalizador.finalizar("sesion");

        assertFalse(grabador.estaGrabando());
        assertEquals(Constantes.TAMANO_ENCABEZADO_GRABACION
                        + Constantes.TAMANO_COMANDO_GRABADO,
                Files.size(archivos.archivoMusica()));
        assertEquals(1, new ListaGrabaciones(
                archivos.archivoLista()).listar().size());
        EntradaIndice indice =
                new BuscadorIndice(
                        archivos.archivoIndice()).buscar("sesion").orElseThrow();
        assertEquals(0, indice.posicionInicial());
        assertEquals(Constantes.TAMANO_ENCABEZADO_GRABACION
                        + Constantes.TAMANO_COMANDO_GRABADO,
                indice.posicionFinal());

        byte[] datosMusica = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] datosEncabezado = new byte[
                Constantes.TAMANO_ENCABEZADO_GRABACION];
        System.arraycopy(datosMusica, 0, datosEncabezado, 0,
                datosEncabezado.length);
        EncabezadoGrabacion encabezado =
                EncabezadoGrabacion.desdeBytes(datosEncabezado);
        assertEquals("sesion", encabezado.nombre());
        assertEquals(1234L, encabezado.fechaEpochMillis());
    }

    @Test
    void duplicadoNoModificaArchivosPersistentes() throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);


        ListaGrabaciones lista =
                new ListaGrabaciones(archivos.archivoLista());
        lista.agregar(new EntradaListaGrabacion("Sesion", 100));
        try (ArchivoRegistros<EntradaIndice> indice =
                crearIndices(archivos.archivoIndice())) {
            indice.escribir(0,
                    new EntradaIndice("Sesion", 0, 224));
        }
        byte[] musicaDuplicada = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] listaDuplicada = Files.readAllBytes(
                archivos.archivoLista());
        byte[] indiceDuplicado = Files.readAllBytes(
                archivos.archivoIndice());

        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(
                        grabador, archivos, () -> 200L);
        Path temporal = grabador.archivoSesionTemporal();

        assertThrows(NombreDuplicadoException.class,
                () -> finalizador.finalizar("SESION"));
        assertFalse(Files.exists(temporal));
        assertArrayEquals(musicaDuplicada,
                Files.readAllBytes(archivos.archivoMusica()));
        assertArrayEquals(listaDuplicada,
                Files.readAllBytes(archivos.archivoLista()));
        assertArrayEquals(indiceDuplicado,
                Files.readAllBytes(archivos.archivoIndice()));
    }

    @Test
    void residuoInvalidaLaSesionSinModificarDatosValidos()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        Path temporal = grabador.archivoSesionTemporal();
        Files.write(grabador.archivoSesionTemporal(),
                new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);

        byte[] musicaAntes = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] listaAntes = Files.readAllBytes(
                archivos.archivoLista());
        byte[] indiceAntes = Files.readAllBytes(
                archivos.archivoIndice());

        assertThrows(IOException.class,
                () -> new FinalizadorGrabacion(
                        grabador, archivos).finalizar("residuo"));

        assertTrue(java.util.Arrays.equals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica())));
        assertTrue(java.util.Arrays.equals(listaAntes,
                Files.readAllBytes(archivos.archivoLista())));
        assertTrue(java.util.Arrays.equals(indiceAntes,
                Files.readAllBytes(archivos.archivoIndice())));
        assertFalse(Files.exists(temporal));
    }

    @Test
    void nombreInvalidoEliminaSoloLaSesionTemporal() throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        Path temporal = grabador.archivoSesionTemporal();
        byte[] musicaAntes = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] listaAntes = Files.readAllBytes(
                archivos.archivoLista());
        byte[] indiceAntes = Files.readAllBytes(
                archivos.archivoIndice());

        assertThrows(ArgumentoInvalidoException.class,
                () -> new FinalizadorGrabacion(
                        grabador, archivos).finalizar(null));

        assertFalse(Files.exists(temporal));
        assertArrayEquals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica()));
        assertArrayEquals(listaAntes,
                Files.readAllBytes(archivos.archivoLista()));
        assertArrayEquals(indiceAntes,
                Files.readAllBytes(archivos.archivoIndice()));
    }

    @Test
    void falloAlRegistrarComandoImpidePersistirLaSesion()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        Path temporal = grabador.archivoSesionTemporal();
        byte[] musicaAntes = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] listaAntes = Files.readAllBytes(
                archivos.archivoLista());
        byte[] indiceAntes = Files.readAllBytes(
                archivos.archivoIndice());

        BusComandos bus = new BusComandos();
        bus.registrarObservador(grabador);
        bus.publicar(new ComandoPruebaFallido(), 11_000_000L);

        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(
                        grabador, archivos, () -> 1234L);
        assertThrows(IllegalStateException.class,
                () -> finalizador.finalizar("sesion-incompleta"));

        assertFalse(Files.exists(temporal));
        assertArrayEquals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica()));
        assertArrayEquals(listaAntes,
                Files.readAllBytes(archivos.archivoLista()));
        assertArrayEquals(indiceAntes,
                Files.readAllBytes(archivos.archivoIndice()));
    }

    @Test
    void restauraElIndiceSiElReemplazoFallaDespuesDeModificarlo()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();
        grabador.comandoEnviado(
                new ComandoPrueba(TipoAccion.PUNTUAL,
                        new double[] {60.0}),
                11_000_000L);

        byte[] musicaAntes = Files.readAllBytes(
                archivos.archivoMusica());
        byte[] listaAntes = Files.readAllBytes(
                archivos.archivoLista());
        byte[] indiceAntes = Files.readAllBytes(
                archivos.archivoIndice());
        Path temporal = grabador.archivoSesionTemporal();

        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(
                        grabador, archivos, () -> 1234L,
                        (indice, resultado, cantidadEsperada) -> {
                            Files.write(indice, new byte[] {1, 2, 3});
                            throw new IOException(
                                    "fallo simulado durante el reemplazo");
                        });

        assertThrows(IOException.class,
                () -> finalizador.finalizar("sesion"));

        assertFalse(Files.exists(temporal));
        assertArrayEquals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica()));
        assertArrayEquals(listaAntes,
                Files.readAllBytes(archivos.archivoLista()));
        assertArrayEquals(indiceAntes,
                Files.readAllBytes(archivos.archivoIndice()));
    }

    @Test
    void limpiezaNoEliminaDirectoriosNiArchivosPersistentes()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        Path directorioTemporal =
                Files.createDirectory(carpetaTemporal.resolve("temporal"));
        IllegalStateException fallo = new IllegalStateException("fallo");

        assertThrows(IllegalArgumentException.class,
                () -> new LimpiezaGrabacion(
                        archivos.archivoMusica(), archivos));

        new LimpiezaGrabacion(directorioTemporal, archivos)
                .limpiar(fallo);

        assertTrue(Files.isDirectory(directorioTemporal));
        assertEquals(1, fallo.getSuppressed().length);
    }


    private static ArchivoRegistros<EntradaIndice>
            crearIndices(Path ruta) {
        return new ArchivoRegistros<>(
                ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes,
                EntradaIndice::serializar);
    }

    private static final class ComandoPrueba implements Comando {

        private final TipoAccion tipoAccion;
        private final double[] parametros;

        private ComandoPrueba(TipoAccion tipoAccion,
                double[] parametros) {
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
        }

        @Override
        public void ejecutar() {
        }

        @Override
        public Sintetizador destino() {
            return new SintetizadorPrueba();
        }

        @Override
        public TipoAccion tipoAccion() {
            return tipoAccion;
        }

        @Override
        public double[] parametros() {
            return parametros.clone();
        }
    }

    private static final class ComandoPruebaFallido implements Comando {

        @Override
        public void ejecutar() {
        }

        @Override
        public Sintetizador destino() {
            throw new IllegalStateException("destino no disponible");
        }

        @Override
        public TipoAccion tipoAccion() {
            return TipoAccion.PUNTUAL;
        }

        @Override
        public double[] parametros() {
            return new double[] {60.0};
        }
    }

    private static final class SintetizadorPrueba
            implements Sintetizador {

        @Override
        public IdSintetizador id() {
            return IdSintetizador.PIANO;
        }

        @Override
        public void ejecutar(TipoAccion tipoAccion,
                double... parametros) {
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
