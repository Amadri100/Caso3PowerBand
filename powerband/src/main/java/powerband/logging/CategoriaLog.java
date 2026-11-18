package powerband.logging;

import powerband.util.Constantes;

public enum CategoriaLog {
    COMANDO(Constantes.CATEGORIA_COMANDO),
    INFO(Constantes.CATEGORIA_INFO),
    ERROR(Constantes.CATEGORIA_ERROR);

    private final String texto;

    CategoriaLog(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }
}
