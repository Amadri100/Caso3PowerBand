package powerband.comandos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BusComandosTest {

    @TempDir
    Path carpetaTemporal;

    @BeforeEach
    void configurarLogger() {
        LoggerPowerBand.cambiarPath(carpetaTemporal);
    }

    @Test
    void delegaElComandoYNotificaUnaVezConElMomentoRecibido() {
        BusComandos bus = new BusComandos();
        SintetizadorDePrueba sintetizador = new SintetizadorDePrueba();
        Comando comando = new ComandoDePrueba(sintetizador,
                TipoAccion.INICIO, new double[] {60.0, 0.75});
        List<Comando> comandosRecibidos = new ArrayList<>();
        List<Long> momentosRecibidos = new ArrayList<>();
        ObservadorComandos observador = (recibido, momento) -> {
            comandosRecibidos.add(recibido);
            momentosRecibidos.add(momento);
            return true;
        };

        bus.registrarObservador(observador);
        bus.registrarObservador(observador);
        comando.ejecutar();
        bus.publicar(comando, 1234L);

        assertEquals(IdSintetizador.PIANO, sintetizador.id());
        assertEquals(TipoAccion.INICIO, sintetizador.tipoAccion);
        assertArrayEquals(new double[] {60.0, 0.75}, sintetizador.parametros);
        assertSame(comando, comandosRecibidos.get(0));
        assertEquals(List.of(1234L), momentosRecibidos);
        assertEquals(1, comandosRecibidos.size());
    }

    @Test
    void quitarObservadorEvitaNotificacionesPosteriores() {
        BusComandos bus = new BusComandos();
        int[] notificaciones = {0};
        ObservadorComandos observador =
                (comando, momento) -> {
                    notificaciones[0]++;
                    return true;
                };

        bus.registrarObservador(observador);
        bus.quitarObservador(observador);
        bus.publicar(comandoDePrueba(), 10L);

        assertEquals(0, notificaciones[0]);
    }

    @Test
    void falloDeObservadorSeRegistraSinImpedirLasDemasNotificaciones()
            throws Exception {
        BusComandos bus = new BusComandos();
        int[] notificaciones = {0};
        bus.registrarObservador((comando, momento) -> {
            throw new IllegalStateException("fallo de prueba");
        });
        bus.registrarObservador((comando, momento) -> {
            notificaciones[0]++;
            return true;
        });

        bus.publicar(comandoDePrueba(), 20L);

        assertEquals(1, notificaciones[0]);
        String contenido = Files.readString(
                carpetaTemporal.resolve("powerband.log"));
        assertTrue(contenido.contains("Fallo al notificar"));
        assertTrue(contenido.contains("fallo de prueba"));
    }

    private static Comando comandoDePrueba() {
        return new ComandoDePrueba(new SintetizadorDePrueba(),
                TipoAccion.PUNTUAL, new double[] {60.0});
    }

    private static final class ComandoDePrueba implements Comando {

        private final Sintetizador destino;
        private final TipoAccion tipoAccion;
        private final double[] parametros;

        private ComandoDePrueba(
                Sintetizador destino, TipoAccion tipoAccion, double[] parametros) {
            this.destino = destino;
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
        }

        @Override
        public void ejecutar() {
            destino.ejecutar(tipoAccion, parametros());
        }

        @Override
        public Sintetizador destino() {
            return destino;
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
            tipoAccion = TipoAccion.FINALIZACION;
            parametros = new double[0];
        }
    }
}
