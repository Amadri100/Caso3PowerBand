package powerband.comandos;

import org.junit.jupiter.api.Test;

import powerband.archivos.ComandoGrabado;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricaComandosGrabadosTest {

    @Test
    void reconstruyeYConservaDestinoAccionYParametros() {
        SintetizadorFalso destino =
                new SintetizadorFalso(IdSintetizador.ORGANO);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(id -> {
                    assertEquals(IdSintetizador.ORGANO, id);
                    return destino;
                });
        ComandoGrabado grabado = new ComandoGrabado(
                250, IdSintetizador.ORGANO, TipoAccion.ACTUALIZACION,
                new double[] {60.5, 0.25, 0.75});

        Comando comando = fabrica.reconstruir(grabado);

        assertSame(destino, comando.destino());
        assertEquals(TipoAccion.ACTUALIZACION, comando.tipoAccion());
        assertArrayEquals(new double[] {60.5, 0.25, 0.75},
                comando.parametros());
    }

    @Test
    void ejecutarDelegaUnaVezYProtegeLosParametrosInternos() {
        SintetizadorFalso destino =
                new SintetizadorFalso(IdSintetizador.BAJO);
        Comando comando = new FabricaComandosGrabados(id -> destino)
                .reconstruir(new ComandoGrabado(
                        0, IdSintetizador.BAJO, TipoAccion.PUNTUAL,
                        new double[] {48.0, 900.0}));

        double[] parametrosExpuestos = comando.parametros();
        parametrosExpuestos[0] = -1.0;
        comando.ejecutar();

        assertEquals(1, destino.cantidadEjecuciones);
        assertEquals(TipoAccion.PUNTUAL, destino.ultimaAccion);
        assertArrayEquals(new double[] {48.0, 900.0},
                destino.ultimosParametros);
        assertArrayEquals(new double[] {48.0, 900.0},
                comando.parametros());
    }

    @Test
    void rechazaRegistroNuloOSintetizadorNoResuelto() {
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(id -> null);

        assertThrows(NullPointerException.class,
                () -> fabrica.reconstruir(null));
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> fabrica.reconstruir(comando(IdSintetizador.PIANO)));
        assertTrue(error.getMessage().contains("PIANO"));
    }

    @Test
    void rechazaSintetizadorConIdentificadorDistinto() {
        FabricaComandosGrabados fabrica = new FabricaComandosGrabados(
                id -> new SintetizadorFalso(IdSintetizador.ORGANO));

        assertThrows(IllegalStateException.class,
                () -> fabrica.reconstruir(comando(IdSintetizador.PIANO)));
    }

    @Test
    void propagaFallosDelResolverYDelSintetizador() {
        IllegalArgumentException falloResolver =
                new IllegalArgumentException("fallo del resolver");
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(id -> {
                    throw falloResolver;
                });
        IllegalArgumentException resultadoResolver = assertThrows(
                IllegalArgumentException.class,
                () -> fabrica.reconstruir(comando(IdSintetizador.PIANO)));
        assertSame(falloResolver, resultadoResolver);

        IllegalStateException falloEjecucion =
                new IllegalStateException("fallo de ejecucion");
        SintetizadorFalso destino =
                new SintetizadorFalso(IdSintetizador.PIANO);
        destino.falloEjecucion = falloEjecucion;
        Comando comando = new FabricaComandosGrabados(id -> destino)
                .reconstruir(comando(IdSintetizador.PIANO));

        assertSame(falloEjecucion,
                assertThrows(IllegalStateException.class, comando::ejecutar));
    }

    @Test
    void rechazaResolverNulo() {
        assertThrows(NullPointerException.class,
                () -> new FabricaComandosGrabados(null));
    }

    private static ComandoGrabado comando(IdSintetizador idSintetizador) {
        return new ComandoGrabado(
                0, idSintetizador, TipoAccion.PUNTUAL, new double[] {60.0});
    }

    private static final class SintetizadorFalso implements Sintetizador {

        private final IdSintetizador id;
        private int cantidadEjecuciones;
        private TipoAccion ultimaAccion;
        private double[] ultimosParametros;
        private IllegalStateException falloEjecucion;

        private SintetizadorFalso(IdSintetizador id) {
            this.id = id;
        }

        @Override
        public IdSintetizador id() {
            return id;
        }

        @Override
        public void ejecutar(TipoAccion tipoAccion, double... parametros) {
            cantidadEjecuciones++;
            ultimaAccion = tipoAccion;
            ultimosParametros = parametros.clone();
            if (falloEjecucion != null) {
                throw falloEjecucion;
            }
            if (parametros.length > 0) {
                parametros[0] = -1.0;
            }
        }

        @Override
        public void liberarTodasLasNotas() {
        }
    }
}
