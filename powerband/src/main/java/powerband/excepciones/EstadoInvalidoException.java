package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorEstado;

public class EstadoInvalidoException extends IllegalStateException
        implements CodigoError {

    private static final long serialVersionUID = 1L;

    private final TipoErrorEstado tipo;

    public EstadoInvalidoException(TipoErrorEstado tipo) {
        super(tipo.codigo() + " " + tipo.plantilla());
        this.tipo = tipo;
    }

    public EstadoInvalidoException(TipoErrorEstado tipo,
            Throwable causa) {
        super(tipo.codigo() + " " + tipo.plantilla(), causa);
        this.tipo = tipo;
    }

    public static EstadoInvalidoException temporalAjeno() {
        return new EstadoInvalidoException(
                TipoErrorEstado.TEMPORAL_AJENO);
    }

    public static EstadoInvalidoException grabacionActiva() {
        return new EstadoInvalidoException(
                TipoErrorEstado.GRABACION_ACTIVA);
    }

    public static EstadoInvalidoException grabacionInactiva() {
        return new EstadoInvalidoException(
                TipoErrorEstado.GRABACION_INACTIVA);
    }

    public final TipoErrorEstado tipo() {
        return tipo;
    }

    @Override
    public final String codigo() {
        return tipo.codigo();
    }

    @Override
    public final String plantilla() {
        return tipo.plantilla();
    }
}
