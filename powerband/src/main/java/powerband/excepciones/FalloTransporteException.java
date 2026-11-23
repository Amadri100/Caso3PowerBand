package powerband.excepciones;

public final class FalloTransporteException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public FalloTransporteException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
