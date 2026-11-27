package powerband.excepciones;

public interface CodigoError {

    public String codigo();

    public String plantilla();

    public default String formatear(Object... argumentos) {
        return String.format(plantilla(), argumentos);
    }
}
