package powerband.controlador;

final class FallosControlador {

    private FallosControlador() {
    }

    static Throwable agregar(Throwable principal, String mensaje, Throwable causa) {
        RuntimeException contextual = new IllegalStateException(mensaje, causa);
        if (principal == null) {
            return contextual;
        }
        if (principal != contextual && principal != causa) {
            principal.addSuppressed(contextual);
        }
        return principal;
    }
}
