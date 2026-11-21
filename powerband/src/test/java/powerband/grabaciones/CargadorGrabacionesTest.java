package powerband.grabaciones;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.archivos.ArchivoRegistros;
import powerband.archivos.ComandoGrabado;
import powerband.archivos.EncabezadoGrabacion;
import powerband.archivos.EntradaIndice;
import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ListaGrabaciones;
import powerband.comandos.TipoAccion;
import powerband.excepciones.ArchivoException;
import powerband.excepciones.ArchivoIncompletoException;
import powerband.excepciones.ArgumentoInvalidoException;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CargadorGrabacionesTest {

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
    void listaCronologicamenteYLeeSoloElIntervaloDeLaSesion()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        ComandoGrabado comandoAnterior = comando(60);
        ComandoGrabado comandoSesion = comando(64);
        ComandoGrabado comandoSiguiente = comando(67);

        byte[] sesionAnterior =
                crearSesion("anterior", 0, comandoAnterior);
        long inicioSesion = sesionAnterior.length;
        byte[] sesion = crearSesion(
                "sesion", inicioSesion, comandoSesion);
        long inicioSiguiente = inicioSesion + sesion.length;
        byte[] sesionSiguiente = crearSesion(
                "siguiente", inicioSiguiente, comandoSiguiente);
        byte[] musica = concatenar(
                sesionAnterior, sesion, sesionSiguiente);
        Files.write(archivos.archivoMusica(), musica);
        guardarIndice(archivos, List.of(
                entrada("anterior", 0, sesionAnterior.length),
                entrada("sesion", inicioSesion,
                        inicioSesion + sesion.length),
                entrada("siguiente", inicioSiguiente,
                        inicioSiguiente + sesionSiguiente.length)));

        ListaGrabaciones lista =
                new ListaGrabaciones(archivos.archivoLista());
        lista.agregar(new EntradaListaGrabacion("siguiente", 30));
        lista.agregar(new EntradaListaGrabacion("sesion", 20));
        lista.agregar(new EntradaListaGrabacion("anterior", 10));

        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        assertEquals(List.of("anterior", "sesion", "siguiente"),
                cargador.listar().stream()
                        .map(EntradaListaGrabacion::nombre).toList());

        byte[] musicaAntes = Files.readAllBytes(
                archivos.archivoMusica());
        try (GrabacionAbierta abierta =
                cargador.abrir("SESION").orElseThrow()) {
            assertEquals("sesion", abierta.encabezado().nombre());
            assertEquals(inicioSesion,
                    abierta.encabezado().posicionInicial());
            assertEquals(inicioSesion + sesion.length,
                    abierta.encabezado().posicionFinal());
            ComandoGrabado comandoLeido =
                    abierta.siguienteComando().orElseThrow();
            assertEquals(comandoSesion.tiempoRelativoMillis(),
                    comandoLeido.tiempoRelativoMillis());
            assertEquals(comandoSesion.idSintetizador(),
                    comandoLeido.idSintetizador());
            assertEquals(comandoSesion.tipoAccion(),
                    comandoLeido.tipoAccion());
            assertArrayEquals(comandoSesion.parametros(),
                    comandoLeido.parametros());
            assertEquals(Optional.empty(), abierta.siguienteComando());
        }
        assertArrayEquals(musicaAntes,
                Files.readAllBytes(archivos.archivoMusica()));
        assertTrue(inicioSesion
                % Constantes.TAMANO_COMANDO_GRABADO != 0);
    }

    @Test
    void devuelveVacioSiLaSesionNoExisteYRechazaNombreInvalido()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);

        assertEquals(Optional.empty(), cargador.abrir("ausente"));
        assertThrows(ArgumentoInvalidoException.class,
                () -> cargador.abrir(""));
    }

    @Test
    void rechazaIntervaloConResiduoAntesDeAbrirLaSesion()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        long posicionFinal =
                Constantes.TAMANO_ENCABEZADO_GRABACION + 1L;
        byte[] encabezado = new EncabezadoGrabacion(
                "sesion", 0, posicionFinal, 100).serializar();
        byte[] musica = new byte[(int) posicionFinal];
        System.arraycopy(encabezado, 0, musica, 0, encabezado.length);
        Files.write(archivos.archivoMusica(), musica);
        guardarIndice(archivos,
                List.of(entrada("sesion", 0, posicionFinal)));

        assertThrows(ArchivoIncompletoException.class,
                () -> new CargadorGrabaciones(archivos).abrir("sesion"));
    }

    @Test
    void rechazaEncabezadoInconsistenteYComandoMalFormado()
            throws IOException {
        ArchivosGrabacion archivos =
                ArchivosGrabacion.inicializar(carpetaTemporal);
        long posicionFinal = Constantes.TAMANO_ENCABEZADO_GRABACION
                + Constantes.TAMANO_COMANDO_GRABADO;
        byte[] encabezado = new EncabezadoGrabacion(
                "otro", 0, posicionFinal, 100).serializar();
        byte[] musica = new byte[(int) posicionFinal];
        System.arraycopy(encabezado, 0, musica, 0, encabezado.length);
        Files.write(archivos.archivoMusica(), musica);
        guardarIndice(archivos,
                List.of(entrada("sesion", 0, posicionFinal)));

        CargadorGrabaciones cargador =
                new CargadorGrabaciones(archivos);
        assertThrows(ArchivoException.class,
                () -> cargador.abrir("sesion"));

        byte[] encabezadoCorrecto = new EncabezadoGrabacion(
                "sesion", 0, posicionFinal, 100).serializar();
        System.arraycopy(encabezadoCorrecto, 0, musica, 0,
                encabezadoCorrecto.length);
        Files.write(archivos.archivoMusica(), musica);

        try (GrabacionAbierta abierta =
                cargador.abrir("sesion").orElseThrow()) {
            assertThrows(ArchivoException.class,
                    abierta::siguienteComando);
        }
    }

    private static ComandoGrabado comando(double nota) {
        return new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                new double[] {nota});
    }

    private static byte[] crearSesion(String nombre, long posicionInicial,
            ComandoGrabado... comandos) throws IOException {
        long posicionFinal = posicionInicial
                + Constantes.TAMANO_ENCABEZADO_GRABACION
                + (long) comandos.length
                        * Constantes.TAMANO_COMANDO_GRABADO;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(new EncabezadoGrabacion(
                nombre, posicionInicial, posicionFinal, 100).serializar());
        for (ComandoGrabado comando : comandos) {
            salida.write(comando.serializar());
        }
        return salida.toByteArray();
    }

    private static EntradaIndice entrada(String nombre,
            long posicionInicial, long posicionFinal) {
        return new EntradaIndice(nombre, posicionInicial, posicionFinal);
    }

    private static void guardarIndice(ArchivosGrabacion archivos,
            List<EntradaIndice> entradas) throws IOException {
        try (ArchivoRegistros<EntradaIndice> indice =
                new ArchivoRegistros<>(
                        archivos.archivoIndice(),
                        Constantes.TAMANO_ENTRADA_INDICE,
                        EntradaIndice::desdeBytes,
                        EntradaIndice::serializar)) {
            for (int indiceActual = 0;
                    indiceActual < entradas.size(); indiceActual++) {
                indice.escribir((long) indiceActual
                        * Constantes.TAMANO_ENTRADA_INDICE,
                        entradas.get(indiceActual));
            }
        }
    }

    private static byte[] concatenar(byte[]... partes)
            throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        for (byte[] parte : partes) {
            salida.write(parte);
        }
        return salida.toByteArray();
    }
}
