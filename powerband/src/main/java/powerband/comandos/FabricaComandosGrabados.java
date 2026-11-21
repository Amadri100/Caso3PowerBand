package powerband.comandos;

import java.util.Objects;
import java.util.function.Function;

import powerband.archivos.ComandoGrabado;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

public final class FabricaComandosGrabados {

    private final Function<IdSintetizador, Sintetizador> resolver;

    public FabricaComandosGrabados(
            Function<IdSintetizador, Sintetizador> resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public Comando reconstruir(ComandoGrabado grabado) {
        Objects.requireNonNull(grabado, "grabado");
        //Obtener el sintetizador
        IdSintetizador idSintetizador = grabado.idSintetizador();
        Sintetizador destino = resolver.apply(idSintetizador);
        if (destino == null || destino.id() != idSintetizador) {
            throw new IllegalStateException(
                    "No se pudo resolver el sintetizador "
                            + idSintetizador);
        }

        return new ComandoReconstruido(destino, grabado.tipoAccion(),
                grabado.parametros());
    }
    // Static no necesita a la clase donde este sea instanciada
    // No significa que no se pueda instanciar como una clase no estatica 
    private static final class ComandoReconstruido implements Comando {

        private final Sintetizador destino;
        private final TipoAccion tipoAccion;
        private final double[] parametros;

        private ComandoReconstruido(Sintetizador destino,
                TipoAccion tipoAccion, double[] parametros) {
            this.destino = destino;
            this.tipoAccion = tipoAccion;
            this.parametros = parametros.clone();
        }

        @Override
        public void ejecutar() {
            destino.ejecutar(tipoAccion, parametros.clone());
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
}
