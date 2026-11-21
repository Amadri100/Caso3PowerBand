package powerband.grabaciones;

import java.util.Objects;

import powerband.comandos.Comando;
import powerband.comandos.TipoAccion;
import powerband.sintetizadores.Sintetizador;

final class ComandoFinalizacion implements Comando {

    private final Sintetizador destino;
    private final double nota;

    ComandoFinalizacion(Sintetizador destino, double nota) {
        this.destino = Objects.requireNonNull(destino, "destino");
        this.nota = nota;
    }

    @Override
    public void ejecutar() {
        destino.ejecutar(TipoAccion.FINALIZACION, nota);
    }

    @Override
    public Sintetizador destino() {
        return destino;
    }

    @Override
    public TipoAccion tipoAccion() {
        return TipoAccion.FINALIZACION;
    }

    @Override
    public double[] parametros() {
        return new double[] {nota};
    }
}
