package powerband.excepciones;

import java.io.IOException;

import powerband.excepciones.tipos.TipoErrorArchivo;

public class ArchivoException extends IOException
        implements CodigoError {

    private static final long serialVersionUID = 1L;

    private final CodigoError tipo;
    private final String ruta;

    protected ArchivoException(CodigoError tipo, String ruta,
            Object... argumentos) {
        super(mensaje(tipo, ruta, argumentos));
        this.tipo = tipo;
        this.ruta = ruta;
    }

    protected ArchivoException(CodigoError tipo, String ruta, Throwable causa,
            Object... argumentos) {
        super(mensaje(tipo, ruta, argumentos), causa);
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public ArchivoException(TipoErrorArchivo tipo, String ruta) {
        super(mensaje(tipo, ruta));
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public ArchivoException(TipoErrorArchivo tipo, String ruta,
            Throwable causa) {
        super(mensaje(tipo, ruta), causa);
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public ArchivoException(TipoErrorArchivo tipo, String ruta,
            long posicion) {
        super(mensaje(tipo, ruta, posicion));
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public ArchivoException(TipoErrorArchivo tipo, String ruta,
            long posicion, long tamano) {
        super(mensaje(tipo, ruta, posicion, tamano));
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public ArchivoException(TipoErrorArchivo tipo, String ruta,
            String valor) {
        super(mensaje(tipo, ruta, valor));
        this.tipo = tipo;
        this.ruta = ruta;
    }

    public static ArchivoException archivoCerrado(
            String ruta) {
        return new ArchivoException(TipoErrorArchivo.ARCHIVO_CERRADO, ruta);
    }

    public static ArchivoException registroFueraDeRango(
            String ruta, long posicion) {
        return new ArchivoException(
                TipoErrorArchivo.REGISTRO_FUERA_DE_RANGO, ruta, posicion);
    }

    public static ArchivoException escrituraConHueco(
            String ruta, long posicion, long tamano) {
        return new ArchivoException(
                TipoErrorArchivo.ESCRITURA_CON_HUECO,
                ruta, posicion, tamano);
    }

    public static ArchivoException reemplazoNoAtomico(
            String ruta) {
        return new ArchivoException(
                TipoErrorArchivo.REEMPLAZO_NO_ATOMICO, ruta,
                ruta);
    }

    public static ArchivoException rutaNoRegular(
            String ruta) {
        return new ArchivoException(
                TipoErrorArchivo.RUTA_NO_REGULAR, ruta);
    }

    public static ArchivoException registroCorrupto(
            String ruta, Throwable causa) {
        return new ArchivoException(
                TipoErrorArchivo.REGISTRO_CORRUPTO, ruta, causa);
    }

    public static ArchivoException errorES(
            String ruta, String operacion, Throwable causa) {
        return new ArchivoException(
                TipoErrorArchivo.ERROR_ES, ruta, causa, operacion);
    }

    public final CodigoError tipo() {
        return tipo;
    }

    public final String ruta() {
        return ruta;
    }

    @Override
    public final String codigo() {
        return tipo.codigo();
    }

    @Override
    public final String plantilla() {
        return tipo.plantilla();
    }

    private static String mensaje(CodigoError tipo, String ruta,
            Object... argumentos) {
        String mensaje = tipo.formatear(argumentos);
        if (ruta == null || ruta.isBlank()) {
            return tipo.codigo() + " " + mensaje;
        }
        return tipo.codigo() + " " + mensaje + " [" + ruta + "]";
    }
}
