package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorOrdenamiento;

public final class CantidadInesperadaException
        extends OrdenamientoException {

    private static final long serialVersionUID = 1L;

    private final long esperada;
    private final long obtenida;

    public CantidadInesperadaException(String ruta, long esperada,
            long obtenida) {
        super(TipoErrorOrdenamiento.CANTIDAD_INESPERADA, ruta,
                esperada, obtenida);
        this.esperada = esperada;
        this.obtenida = obtenida;
    }

    public CantidadInesperadaException(long esperada, long obtenida,
            String ruta) {
        this(ruta, esperada, obtenida);
    }

    public long esperada() {
        return esperada;
    }

    public long obtenida() {
        return obtenida;
    }
}
