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
}
