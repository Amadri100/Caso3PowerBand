package powerband.excepciones.tipos;

import powerband.excepciones.CodigoError;
import powerband.excepciones.ConstantesExcepciones;

import static powerband.excepciones.ConstantesExcepciones.PREFIJO_ARGUMENTO;

public enum TipoErrorArgumento implements CodigoError {
    NOMBRE_INVALIDO(PREFIJO_ARGUMENTO + "001",
            ConstantesExcepciones.MSG_NOMBRE_INVALIDO),
    OFFSET_INVALIDO(PREFIJO_ARGUMENTO + "002",
            ConstantesExcepciones.MSG_OFFSET_INVALIDO),
    INTERVALO_INVALIDO(PREFIJO_ARGUMENTO + "003",
            ConstantesExcepciones.MSG_INTERVALO_INVALIDO),
    TAMANO_REGISTRO_INVALIDO(PREFIJO_ARGUMENTO + "004",
            ConstantesExcepciones.MSG_TAMANO_REGISTRO_INVALIDO),
    PARAMETROS_EXCESO(PREFIJO_ARGUMENTO + "005",
            ConstantesExcepciones.MSG_PARAMETROS_EXCESO);

    private final String codigo;
    private final String plantilla;

    TipoErrorArgumento(String codigo, String plantilla) {
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
