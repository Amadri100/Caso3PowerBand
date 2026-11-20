package powerband.excepciones.tipos;

import powerband.excepciones.CodigoError;
import powerband.excepciones.ConstantesExcepciones;

import static powerband.excepciones.ConstantesExcepciones.PREFIJO_ORDENAMIENTO;

public enum TipoErrorOrdenamiento implements CodigoError {
    NOMBRE_DUPLICADO(PREFIJO_ORDENAMIENTO + "001",
            ConstantesExcepciones.MSG_NOMBRE_DUPLICADO),
    INDICE_DESORDENADO(PREFIJO_ORDENAMIENTO + "002",
            ConstantesExcepciones.MSG_INDICE_DESORDENADO),
    CANTIDAD_INESPERADA(PREFIJO_ORDENAMIENTO + "003",
            ConstantesExcepciones.MSG_CANTIDAD_INESPERADA),
    LIMPIEZA_FALLIDA(PREFIJO_ORDENAMIENTO + "004",
            ConstantesExcepciones.MSG_LIMPIEZA_FALLIDA);

    private final String codigo;
    private final String plantilla;

    TipoErrorOrdenamiento(String codigo, String plantilla) {
        this.codigo = codigo;
        this.plantilla = plantilla;
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public String plantilla() {
        return plantilla;
    }
}
