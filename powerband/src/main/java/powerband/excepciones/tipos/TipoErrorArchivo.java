package powerband.excepciones.tipos;

import powerband.excepciones.CodigoError;
import powerband.excepciones.ConstantesExcepciones;

import static powerband.excepciones.ConstantesExcepciones.PREFIJO_ARCHIVO;

public enum TipoErrorArchivo implements CodigoError {
    ARCHIVO_CERRADO(PREFIJO_ARCHIVO + "001",
            ConstantesExcepciones.MSG_ARCHIVO_CERRADO),
    ARCHIVO_INCOMPLETO(PREFIJO_ARCHIVO + "002",
            ConstantesExcepciones.MSG_ARCHIVO_INCOMPLETO),
    REGISTRO_FUERA_DE_RANGO(PREFIJO_ARCHIVO + "003",
            ConstantesExcepciones.MSG_REGISTRO_FUERA_DE_RANGO),
    REGISTRO_CORRUPTO(PREFIJO_ARCHIVO + "004",
            ConstantesExcepciones.MSG_REGISTRO_CORRUPTO),
    ESCRITURA_CON_HUECO(PREFIJO_ARCHIVO + "005",
            ConstantesExcepciones.MSG_ESCRITURA_CON_HUECO),
    REEMPLAZO_NO_ATOMICO(PREFIJO_ARCHIVO + "006",
            ConstantesExcepciones.MSG_REEMPLAZO_NO_ATOMICO),
    RUTA_NO_REGULAR(PREFIJO_ARCHIVO + "007",
            ConstantesExcepciones.MSG_RUTA_NO_REGULAR),
    ERROR_ES(PREFIJO_ARCHIVO + "099",
            ConstantesExcepciones.MSG_ERROR_ES);

    private final String codigo;
    private final String plantilla;

    TipoErrorArchivo(String codigo, String plantilla) {
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
