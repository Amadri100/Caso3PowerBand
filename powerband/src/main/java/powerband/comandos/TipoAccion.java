package powerband.comandos;

public enum TipoAccion {
    PUNTUAL(1),
    INICIO(2),
    ACTUALIZACION(3),
    FINALIZACION(4);

    private final int codigo;

    TipoAccion(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static TipoAccion desdeCodigo(int codigo) {
        for (TipoAccion tipoAccion : values()) {
            if (tipoAccion.codigo == codigo) {
                return tipoAccion;
            }
        }
        throw new IllegalArgumentException(
                "Codigo de accion desconocido: " + codigo);
    }
}
