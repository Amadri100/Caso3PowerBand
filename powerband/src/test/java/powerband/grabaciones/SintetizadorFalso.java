package powerband.grabaciones;

import java.util.ArrayList;
import java.util.List;

import powerband.comandos.TipoAccion;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

final class SintetizadorFalso implements Sintetizador {

    private final IdSintetizador id;
    final List<Double> parametrosEjecutados = new ArrayList<>();
    final List<TipoAccion> accionesEjecutadas = new ArrayList<>();
    final List<double[]> parametrosPorEjecucion = new ArrayList<>();
    RuntimeException falloEjecucion;
    RuntimeException falloPuntual;
    RuntimeException falloFinalizacion;

    SintetizadorFalso(IdSintetizador id) {
        this.id = id;
    }

    @Override
    public IdSintetizador id() {
        return id;
    }

    @Override
    public void ejecutar(TipoAccion tipoAccion, double... parametros) {
        if (falloEjecucion != null) {
            throw falloEjecucion;
        }
        if (tipoAccion == TipoAccion.PUNTUAL && falloPuntual != null) {
            throw falloPuntual;
        }
        if (tipoAccion == TipoAccion.FINALIZACION
                && falloFinalizacion != null) {
            throw falloFinalizacion;
        }
        accionesEjecutadas.add(tipoAccion);
        parametrosPorEjecucion.add(parametros.clone());
        parametrosEjecutados.add(parametros[0]);
    }

    @Override
    public void liberarTodasLasNotas() {
    }
}
