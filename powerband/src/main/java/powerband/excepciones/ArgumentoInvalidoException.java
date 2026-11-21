package powerband.excepciones;

import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.util.Constantes;

public class ArgumentoInvalidoException extends IllegalArgumentException
        implements CodigoError {

    private static final long serialVersionUID = 1L;

    private final TipoErrorArgumento tipo;

    protected ArgumentoInvalidoException(TipoErrorArgumento tipo,
            Object... argumentos) {
        super(tipo.codigo() + " " + tipo.formatear(argumentos));
        this.tipo = tipo;
    }

    protected ArgumentoInvalidoException(TipoErrorArgumento tipo,
            Throwable causa, Object... argumentos) {
        super(tipo.codigo() + " " + tipo.formatear(argumentos), causa);
        this.tipo = tipo;
    }

    protected ArgumentoInvalidoException(TipoErrorArgumento tipo,
            Throwable causa, String detalle) {
        super(tipo.codigo() + " " + detalle, causa);
        this.tipo = tipo;
    }

    public ArgumentoInvalidoException() {
        this(TipoErrorArgumento.NOMBRE_INVALIDO,
                Constantes.LONGITUD_MAXIMA_NOMBRE);
    }

    public ArgumentoInvalidoException(TipoErrorArgumento tipo,
            String detalle) {
        super(tipo.codigo() + " " + detalle);
        this.tipo = tipo;
    }

    public static ArgumentoInvalidoException nombreInvalido() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                Constantes.LONGITUD_MAXIMA_NOMBRE);
    }

    public static ArgumentoInvalidoException nombreInvalido(
            Throwable causa) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO, causa,
                Constantes.LONGITUD_MAXIMA_NOMBRE);
    }

    public static ArgumentoInvalidoException nombreNulo() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                ConstantesExcepciones.MSG_NOMBRE_NULO);
    }

    public static ArgumentoInvalidoException nombreConNulos() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                ConstantesExcepciones.MSG_NOMBRE_NULO_BAJO);
    }

    public static ArgumentoInvalidoException nombreUtf8Invalido(
            Throwable causa) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO, causa,
                ConstantesExcepciones.MSG_NOMBRE_UTF8_INVALIDO);
    }

    public static ArgumentoInvalidoException rellenoNombreInvalido() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                ConstantesExcepciones.MSG_RELLENO_NOMBRE_INVALIDO);
    }

    public static ArgumentoInvalidoException nombreVacio() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                ConstantesExcepciones.MSG_NOMBRE_VACIO);
    }

    public static ArgumentoInvalidoException nombreCodificadoInvalido(
            Throwable causa) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO, causa,
                ConstantesExcepciones.MSG_NOMBRE_CODIFICADO_INVALIDO);
    }

    public static ArgumentoInvalidoException offsetInvalido(
            int tamanoRegistro, long posicion) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.OFFSET_INVALIDO,
                tamanoRegistro, posicion);
    }

    public static ArgumentoInvalidoException intervaloInvalido(
            long inicio, long fin) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.INTERVALO_INVALIDO, inicio, fin);
    }

    public static ArgumentoInvalidoException tamanoRegistroInvalido() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.TAMANO_REGISTRO_INVALIDO);
    }

    public static ArgumentoInvalidoException detalle(
            TipoErrorArgumento tipo, String detalle) {
        return new ArgumentoInvalidoException(tipo, detalle);
    }

    public static ArgumentoInvalidoException tiempoRelativoNegativo() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.PARAMETROS_EXCESO,
                ConstantesExcepciones.MSG_TIEMPO_RELATIVO_NEGATIVO);
    }

    public static ArgumentoInvalidoException parametrosNoUtilizados() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.PARAMETROS_EXCESO,
                ConstantesExcepciones.MSG_PARAMETROS_NO_UTILIZADOS);
    }

    public static ArgumentoInvalidoException parametrosExceso(
            int maximo, int cantidad) {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.PARAMETROS_EXCESO, maximo, cantidad);
    }

    public static ArgumentoInvalidoException comandoNulo() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.NOMBRE_INVALIDO,
                ConstantesExcepciones.MSG_COMANDO_NULO);
    }

    public static ArgumentoInvalidoException momentoAnterior() {
        return new ArgumentoInvalidoException(
                TipoErrorArgumento.INTERVALO_INVALIDO,
                ConstantesExcepciones.MSG_MOMENTO_ANTERIOR);
    }

    public final TipoErrorArgumento tipo() {
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
