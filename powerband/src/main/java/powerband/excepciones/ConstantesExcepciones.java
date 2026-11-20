package powerband.excepciones;

public final class ConstantesExcepciones {

    public static final String PREFIJO_ARCHIVO = "ARC-";
    public static final String PREFIJO_ORDENAMIENTO = "ORD-";
    public static final String PREFIJO_ARGUMENTO = "ARG-";
    public static final String PREFIJO_ESTADO = "EST-";

    public static final String MSG_ARCHIVO_CERRADO =
            "El archivo de registros está cerrado";
    public static final String MSG_ARCHIVO_INCOMPLETO =
            "Archivo incompleto: tamaño %d bytes, registro de %d bytes, residuo %d";
    public static final String MSG_REGISTRO_FUERA_DE_RANGO =
            "No hay un registro completo en el offset %d";
    public static final String MSG_REGISTRO_CORRUPTO =
            "El registro no cumple su formato";
    public static final String MSG_ESCRITURA_CON_HUECO =
            "El offset %d crearía un espacio después del final del archivo (%d bytes)";
    public static final String MSG_REEMPLAZO_NO_ATOMICO =
            "El sistema de archivos no permite reemplazar %s de forma atómica";
    public static final String MSG_RUTA_NO_REGULAR =
            "La ruta requerida no es un archivo regular";
    public static final String MSG_ERROR_ES =
            "No se pudo %s";

    public static final String MSG_NOMBRE_DUPLICADO =
            "El índice contiene nombres duplicados: %s / %s";
    public static final String MSG_INDICE_DESORDENADO =
            "El índice no quedó estrictamente ordenado cerca de '%s'";
    public static final String MSG_CANTIDAD_INESPERADA =
            "Se esperaban %d registros y se obtuvieron %d";
    public static final String MSG_LIMPIEZA_FALLIDA =
            "No se pudo eliminar el archivo temporal %s";

    public static final String MSG_NOMBRE_INVALIDO =
            "El nombre debe ocupar entre 1 y %d bytes UTF-8";
    public static final String MSG_NOMBRE_NULO =
            "El nombre no puede ser nulo";
    public static final String MSG_NOMBRE_NULO_BAJO =
            "El nombre no puede contener bytes nulos";
    public static final String MSG_NOMBRE_UTF8_INVALIDO =
            "El nombre no es una cadena UTF-8 válida";
    public static final String MSG_RELLENO_NOMBRE_INVALIDO =
            "El relleno del nombre debe contener ceros";
    public static final String MSG_NOMBRE_VACIO =
            "El nombre no puede estar vacío";
    public static final String MSG_NOMBRE_CODIFICADO_INVALIDO =
            "El nombre codificado no es UTF-8 válido";
    public static final String MSG_OFFSET_INVALIDO =
            "El offset debe ser no negativo y múltiplo de %d: %d";
    public static final String MSG_INTERVALO_INVALIDO =
            "El intervalo no es válido: [%d, %d]";
    public static final String MSG_TAMANO_REGISTRO_INVALIDO =
            "El tamaño del registro debe ser positivo";
    public static final String MSG_PARAMETROS_EXCESO =
            "Cantidad de parámetros fuera del máximo de %d: %d";
    public static final String MSG_TIEMPO_RELATIVO_NEGATIVO =
            "El tiempo relativo no puede ser negativo";
    public static final String MSG_PARAMETROS_NO_UTILIZADOS =
            "Los parámetros no utilizados deben ser cero";
    public static final String MSG_ENTRADA_INDICE_LONGITUD =
            "La entrada de índice debe tener %d bytes";
    public static final String MSG_ENCABEZADO_LONGITUD =
            "El encabezado debe tener %d bytes";
    public static final String MSG_ENTRADA_LISTA_LONGITUD =
            "La entrada de lista debe tener %d bytes";
    public static final String MSG_COMANDO_LONGITUD =
            "El comando grabado debe tener %d bytes";

    public static final String MSG_TEMPORAL_AJENO =
            "El resultado no pertenece a los temporales del ordenamiento";

    private ConstantesExcepciones() {
    }
}
