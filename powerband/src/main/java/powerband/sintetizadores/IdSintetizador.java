package powerband.sintetizadores;

import powerband.util.Constantes;

public enum IdSintetizador {
    PIANO(Constantes.SINTETIZADORES_INDICES[0]),
    BAJO(Constantes.SINTETIZADORES_INDICES[1]),
    ORGANO(Constantes.SINTETIZADORES_INDICES[2]);

    private final int codigo;

    IdSintetizador(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public static IdSintetizador desdeCodigo(int codigo) {
        for (IdSintetizador sintetizador : values()) {
            if (sintetizador.codigo == codigo) {
                return sintetizador;
            }
        }
        throw new IllegalArgumentException(
                "Codigo de sintetizador desconocido: " + codigo);
    }
}
