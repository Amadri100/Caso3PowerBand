package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorOrdenamiento;

public class OrdenamientoException
        extends ArchivoException {

    private static final long serialVersionUID = 1L;

    public OrdenamientoException(TipoErrorOrdenamiento tipo,
            String ruta, String nombre) {
        super(tipo, ruta, nombre);
    }

    public OrdenamientoException(TipoErrorOrdenamiento tipo,
            String ruta, String nombre1, String nombre2) {
        super(tipo, ruta, nombre1, nombre2);
    }

    public OrdenamientoException(TipoErrorOrdenamiento tipo,
            String ruta, long esperada, long obtenida) {
        super(tipo, ruta, esperada, obtenida);
    }

    public TipoErrorOrdenamiento tipoOrdenamiento() {
        return (TipoErrorOrdenamiento) tipo();
    }

    public static OrdenamientoException indiceDesordenado(
            String ruta, String nombre) {
        return new OrdenamientoException(
                TipoErrorOrdenamiento.INDICE_DESORDENADO,
                ruta, nombre);
    }
}
