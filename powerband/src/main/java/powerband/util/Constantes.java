package powerband.util;

import java.nio.ByteOrder;

public final class Constantes {

    public static final ByteOrder ORDEN_BYTES_PROGRAMA = ByteOrder.BIG_ENDIAN;

    public static final String HOST_DEFECTO = "127.0.0.1";
    public static final int PUERTO_DEFECTO = 57117;
    public static final int PUERTO_UDP = PUERTO_DEFECTO;

    public static final String RUTA_LOGS = "powerband/logs";
    public static final String RUTA_GRABACIONES = "powerband/recordings";
    public static final String NOMBRE_ARCHIVO_GRABACIONES_MUSICA =
            "recordings.music";
    public static final String NOMBRE_ARCHIVO_GRABACIONES_LISTA =
            "recordings.list";
    public static final String NOMBRE_ARCHIVO_GRABACIONES_INDICE =
            "recordings.index";

    public static final String CATEGORIA_COMANDO = "comando";
    public static final String CATEGORIA_INFO = "info";
    public static final String CATEGORIA_ERROR = "error";

    public static final int NOTA_MIDI_MINIMA = 0;
    public static final int NOTA_MIDI_MAXIMA = 127;
    public static final int DURACION_MINIMA_MS = 1;
    public static final int DURACION_MAXIMA_MS = Integer.MAX_VALUE;
    public static final int TERCERA_MENOR_SEMITONOS = 3;
    public static final int TERCERA_MAYOR_SEMITONOS = 4;
    public static final int CANTIDAD_PARCIALES_ORGANO = 3;
    public static final double RAZON_ORGANO_1 = 1.0;
    public static final double RAZON_ORGANO_2 = 2.0;
    public static final double RAZON_ORGANO_3 = 3.0;
    public static final double AMPLITUD_ORGANO_1 = 1.0;
    public static final double AMPLITUD_ORGANO_2 = 0.5;
    public static final double AMPLITUD_ORGANO_3 = 0.25;
    public static final double AMPLITUD_MINIMA = 0.0;
    public static final double AMPLITUD_MAXIMA = 1.0;
    public static final double FRECUENCIA_AFINACION_HZ = 440.0;
    public static final int SEMITONOS_POR_OCTAVA = 12;
    public static final double VALOR_MUSICAL_MINIMO = 0.0;
    public static final double VALOR_MUSICAL_MAXIMO = 1.0;
    public static final double FRECUENCIA_MINIMA_HZ = 20.0;
    public static final double FRECUENCIA_MAXIMA_HZ = 20000.0;
    public static final double PRESION_MINIMA = 0.0;
    public static final double PRESION_MAXIMA = 1.0;
    public static final int TAMANO_BUFFER_AUDIO = 1024;
    public static final int SUPERPOSICION_AUDIO = 0;

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
    public static final int CAPACIDAD_BUFFER_REPRODUCCION = 32;

    public static final int TAMANO_ENCABEZADO_GRABACION =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_POSICION_BYTES
                    + TAMANO_POSICION_BYTES + TAMANO_FECHA_EPOCH_MILLIS;
    public static final int TAMANO_COMANDO_GRABADO =
            TAMANO_TIEMPO_RELATIVO + TAMANO_ID_SINTETIZADOR
                    + TAMANO_TIPO_ACCION + TAMANO_CANTIDAD_PARAMETROS
                    + MAXIMO_PARAMETROS_COMANDO * TAMANO_PARAMETRO_COMANDO;
    public static final int TAMANO_ENTRADA_INDICE =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_POSICION_BYTES + TAMANO_POSICION_BYTES;
    public static final int TAMANO_ENTRADA_LISTA_GRABACION =
            LONGITUD_MAXIMA_NOMBRE + TAMANO_FECHA_EPOCH_MILLIS;

    private Constantes() {
    }
}
