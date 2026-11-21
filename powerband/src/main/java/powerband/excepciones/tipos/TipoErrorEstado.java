package powerband.excepciones.tipos;

import powerband.excepciones.CodigoError;
import powerband.excepciones.ConstantesExcepciones;

import static powerband.excepciones.ConstantesExcepciones.PREFIJO_ESTADO;

public enum TipoErrorEstado implements CodigoError {
    TEMPORAL_AJENO(PREFIJO_ESTADO + "001",
            ConstantesExcepciones.MSG_TEMPORAL_AJENO),
    GRABACION_ACTIVA(PREFIJO_ESTADO + "002",
            ConstantesExcepciones.MSG_GRABACION_ACTIVA),
    GRABACION_INACTIVA(PREFIJO_ESTADO + "003",
            ConstantesExcepciones.MSG_GRABACION_INACTIVA);

    private final String codigo;
    private final String plantilla;

    TipoErrorEstado(String codigo, String plantilla) {
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
