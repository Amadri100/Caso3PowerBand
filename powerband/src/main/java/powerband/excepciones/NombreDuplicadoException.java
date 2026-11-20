package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorOrdenamiento;

public final class NombreDuplicadoException
        extends OrdenamientoException {

    private static final long serialVersionUID = 1L;

    private final String nombre1;
    private final String nombre2;

    public NombreDuplicadoException(String nombre1, String nombre2) {
        super(TipoErrorOrdenamiento.NOMBRE_DUPLICADO, null,
                nombre1, nombre2);
        this.nombre1 = nombre1;
        this.nombre2 = nombre2;
    }

    public NombreDuplicadoException(String nombre1, String nombre2,
            String ruta) {
        super(TipoErrorOrdenamiento.NOMBRE_DUPLICADO, ruta,
                nombre1, nombre2);
        this.nombre1 = nombre1;
        this.nombre2 = nombre2;
    }

    public String nombre1() {
        return nombre1;
    }

    public String nombre2() {
        return nombre2;
    }
}
