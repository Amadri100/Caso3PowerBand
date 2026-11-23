package powerband.comandos;

import java.nio.file.Path;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandosTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @Test
    void comandoPuntualDelegaLosDatosSinExponerElArregloInterno() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        double[] datos = {60.0, 500.0};

        ComandoPuntual comando = new ComandoPuntual(sintetizador, datos);
        datos[0] = 1.0;
        comando.ejecutar();

        assertEquals(TipoAccion.PUNTUAL, sintetizador.tipoAccion);
        assertArrayEquals(new double[] {60.0, 500.0},
                sintetizador.parametros);
        double[] consultados = comando.parametros();
        consultados[0] = 2.0;
        assertArrayEquals(new double[] {60.0, 500.0},
                comando.parametros());
    }

    @Test
    void comandosSostenidosConservanElTipoYLaIdentificacion() {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();

        ComandoInicio inicio =
                new ComandoInicio(sintetizador, "nota-1", 60.0);
        ComandoActualizacion actualizacion =
                new ComandoActualizacion(sintetizador, "nota-1", 62.0);
        ComandoFinalizacion finalizacion =
                new ComandoFinalizacion(sintetizador, "nota-1", 60.0);

        inicio.ejecutar();
        assertEquals(TipoAccion.INICIO, sintetizador.tipoAccion);
        actualizacion.ejecutar();
        assertEquals(TipoAccion.ACTUALIZACION, sintetizador.tipoAccion);
        finalizacion.ejecutar();
        assertEquals(TipoAccion.FINALIZACION, sintetizador.tipoAccion);
        assertEquals("nota-1", inicio.identificador());
        assertArrayEquals(new double[] {60.0}, sintetizador.parametros);
        assertTrue(inicio.toString().contains("identificadorAccion=nota-1"));
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoInicio(sintetizador, " ", 60.0));
    }

    @Test
    void comandosPuntualesYSostenidosUsanElSintetizadorFalsoYSeRegistran()
            throws Exception {
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();

        new ComandoPuntual(sintetizador, 48.0, 0.75).ejecutar();
        new ComandoInicio(sintetizador, "nota-2", 60.0).ejecutar();
        new ComandoActualizacion(sintetizador, "nota-2", 62.0).ejecutar();
        new ComandoFinalizacion(sintetizador, "nota-2", 62.0).ejecutar();

        String log = Files.readString(
                carpetaTemporal.resolve("powerband.log"));
        assertEquals(4, log.lines()
                .filter(linea -> linea.contains("[comando]"))
                .count());
        assertTrue(log.contains("ComandoPuntual"));
        assertTrue(log.contains("ComandoInicio"));
        assertTrue(log.contains("ComandoActualizacion"));
        assertTrue(log.contains("ComandoFinalizacion"));
    }

    private static final class SintetizadorDePrueba implements Sintetizador {

        private TipoAccion tipoAccion;
        private double[] parametros;

        @Override
        public IdSintetizador id() {
            return IdSintetizador.PIANO;
        }

        @Override
        public void ejecutar(TipoAccion tipoAccion, double... parametros) {
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
