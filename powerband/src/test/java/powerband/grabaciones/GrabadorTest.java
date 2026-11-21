package powerband.grabaciones;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.archivos.ArchivoRegistros;
import powerband.archivos.ComandoGrabado;
import powerband.comandos.Comando;
import powerband.comandos.TipoAccion;
import powerband.excepciones.EstadoInvalidoException;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrabadorTest {

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
    void iniciaDetieneYRechazaOperacionesRepetidas() throws IOException {
        AtomicLong reloj = new AtomicLong(100_000L);
        Grabador grabador = new Grabador(carpetaTemporal, reloj::get);

        grabador.iniciar();
        assertTrue(grabador.estaGrabando());
        assertEquals(0, grabador.cantidadComandos());
        assertEquals(0, Files.size(grabador.archivoSesionTemporal()));

        assertThrows(EstadoInvalidoException.class, grabador::iniciar);
        grabador.detener();

        assertFalse(grabador.estaGrabando());
        assertEquals(0, grabador.cantidadComandos());
        assertThrows(EstadoInvalidoException.class, grabador::detener);
        assertEquals(100_000L, reloj.get());
    }

    @Test
    void registraTiempoRelativoComandosYParametros() throws IOException {
        AtomicLong reloj = new AtomicLong(10_000_000L);
        Grabador grabador = new Grabador(carpetaTemporal, reloj::get);
        grabador.iniciar();

        grabador.comandoEnviado(
                new ComandoPrueba(TipoAccion.INICIO,
                        new double[] {60.0, 0.75}),
                11_500_999L);

        assertEquals(1, grabador.cantidadComandos());

        try (ArchivoRegistros<ComandoGrabado> archivo =
                crearArchivo(grabador.archivoSesionTemporal())) {
            ComandoGrabado registro = archivo.leer(0);
            assertEquals(1, registro.tiempoRelativoMillis());
            assertEquals(IdSintetizador.PIANO,
                    registro.idSintetizador());
            assertEquals(TipoAccion.INICIO, registro.tipoAccion());
            assertArrayEquals(new double[] {60.0, 0.75},
                    registro.parametros());
        }

        grabador.detener();
    }

    @Test
    void rechazaMomentosAnteriorAlInicio() throws IOException {
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 10_000_000L);
        grabador.iniciar();

        assertFalse(grabador.comandoEnviado(
                new ComandoPrueba(TipoAccion.PUNTUAL,
                        new double[] {60.0}),
                9_999_999L));
        assertEquals(0, grabador.cantidadComandos());
        grabador.detener();
    }

    @Test
    void ignoraComandosDespuesDeDetener() throws IOException {
        Grabador grabador = new Grabador(
                carpetaTemporal, () -> 50_000_000L);
        grabador.iniciar();
        grabador.detener();

        grabador.comandoEnviado(
                new ComandoPrueba(TipoAccion.PUNTUAL,
                        new double[] {60.0}),
                51_000_000L);

        assertFalse(Files.isDirectory(grabador.archivoSesionTemporal()));
    }

    private ArchivoRegistros<ComandoGrabado> crearArchivo(Path ruta) {
        return new ArchivoRegistros<>(
                ruta,
                Constantes.TAMANO_COMANDO_GRABADO,
                ComandoGrabado::desdeBytes,
                ComandoGrabado::serializar);
    }

    private static void assertArrayEquals(double[] esperado,
            double[] obtenido) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                esperado, obtenido);
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
        public powerband.sintetizadores.Sintetizador destino() {
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
