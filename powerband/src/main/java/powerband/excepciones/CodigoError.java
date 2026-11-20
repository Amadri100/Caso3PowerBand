package powerband.excepciones;

public interface CodigoError {

    String codigo();

    String plantilla();

    default String formatear(Object... argumentos) {
        return String.format(plantilla(), argumentos);
    }
}
