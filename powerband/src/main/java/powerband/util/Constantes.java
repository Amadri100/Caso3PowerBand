package powerband.util;

import java.nio.ByteOrder;

public final class Constantes {

    public static final ByteOrder ORDEN_BYTES_PROGRAMA = ByteOrder.BIG_ENDIAN;

    public static final String HOST_DEFECTO = "127.0.0.1";
    public static final int PUERTO_DEFECTO = 57117;
    public static final int PUERTO_UDP = PUERTO_DEFECTO;

    public static final String RUTA_LOGS = "powerband/logs";
    public static final String RUTA_GRABACIONES = "powerband/recordings";

    public static final String CATEGORIA_COMANDO = "comando";
    public static final String CATEGORIA_INFO = "info";
    public static final String CATEGORIA_ERROR = "error";

    public static final int[] SINTETIZADORES_INDICES = {1,2,3}; //Piano, bajo, organo

    public static final int LONGITUD_MAXIMA_NOMBRE = 200;
    public static final int TAMANO_POSICION_BYTES = 8;
    public static final int TAMANO_FECHA_EPOCH_MILLIS = 8;
    public static final int TAMANO_TIEMPO_RELATIVO = 8;
    public static final int TAMANO_ID_SINTETIZADOR = 1;
    public static final int TAMANO_TIPO_ACCION = 1;
    public static final int TAMANO_CANTIDAD_PARAMETROS = 1;
    public static final int MAXIMO_PARAMETROS_COMANDO = 8;
    public static final int TAMANO_PARAMETRO_COMANDO = Double.BYTES;

    public static final int TAMANO_ENCABEZADO_GRABACION =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_POSICION_BYTES
                    + TAMANO_POSICION_BYTES + TAMANO_FECHA_EPOCH_MILLIS;
    public static final int TAMANO_COMANDO_GRABADO =
            TAMANO_TIEMPO_RELATIVO + TAMANO_ID_SINTETIZADOR
                    + TAMANO_TIPO_ACCION + TAMANO_CANTIDAD_PARAMETROS
                    + MAXIMO_PARAMETROS_COMANDO * TAMANO_PARAMETRO_COMANDO;
    public static final int TAMANO_ENTRADA_INDICE =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_POSICION_BYTES + TAMANO_POSICION_BYTES;

    private Constantes() {
    }
}
