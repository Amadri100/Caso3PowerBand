package powerband.util;

public final class Constantes {

    public static final String HOST_DEFECTO = "127.0.0.1";
    public static final int PUERTO_DEFECTO = 57117;
    public static final int PUERTO_UDP = PUERTO_DEFECTO;

    public static final String RUTA_LOGS = "powerband/logs";
    public static final String RUTA_GRABACIONES = "powerband/recordings";

    public static final String CATEGORIA_COMANDO = "comando";
    public static final String CATEGORIA_INFO = "info";
    public static final String CATEGORIA_ERROR = "error";

    public static final int[] SINTETIZADORES_INDICES = {1,2,3}; //Piano, bajo, organo

    public static final int LONGITUD_MAXIMA_NOMBRE = 64;
    public static final int LONGITUD_FECHA = 24;
    public static final int TAMANO_POSICION_BYTES = 8;
    public static final int TAMANO_TIEMPO_RELATIVO = 8;
    public static final int TAMANO_ID_SINTETIZADOR = 4;
    public static final int TAMANO_TIPO_ACCION = 4;
    public static final int TAMANO_CANTIDAD_PARAMETROS = 4;

    public static final int TAMANO_ENCABEZADO_GRABACION =
            LONGITUD_MAXIMA_NOMBRE + LONGITUD_FECHA
                    + TAMANO_POSICION_BYTES + TAMANO_POSICION_BYTES;
    public static final int TAMANO_COMANDO_GRABADO =
            TAMANO_TIEMPO_RELATIVO + TAMANO_ID_SINTETIZADOR
                    + TAMANO_TIPO_ACCION + TAMANO_CANTIDAD_PARAMETROS;
    public static final int TAMANO_ENTRADA_INDICE =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_POSICION_BYTES + TAMANO_POSICION_BYTES;

    private Constantes() {
    }
}
