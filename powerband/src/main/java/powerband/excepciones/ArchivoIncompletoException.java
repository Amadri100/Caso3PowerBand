package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorArchivo;

public final class ArchivoIncompletoException
        extends ArchivoException {

    private static final long serialVersionUID = 1L;

    private final long tamano;
    private final long tamanoRegistro;
    private final long residuo;

    public ArchivoIncompletoException(String ruta, long tamano,
            long tamanoRegistro) {
        super(TipoErrorArchivo.ARCHIVO_INCOMPLETO, ruta,
                tamano, tamanoRegistro, tamano % tamanoRegistro);
        this.tamano = tamano;
        this.tamanoRegistro = tamanoRegistro;
        this.residuo = tamano % tamanoRegistro;
    }

    public ArchivoIncompletoException(long tamano, long tamanoRegistro,
            String ruta) {
        this(ruta, tamano, tamanoRegistro);
    }

    public long tamano() {
        return tamano;
    }

    public long tamanoRegistro() {
        return tamanoRegistro;
    }

    public long residuo() {
        return residuo;
    }
}
