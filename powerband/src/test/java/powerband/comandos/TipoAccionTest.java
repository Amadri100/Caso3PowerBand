package powerband.comandos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TipoAccionTest {

    @Test
    void exponeCodigosPersistentesEstables() {
        assertEquals(4, TipoAccion.values().length);
        assertEquals(1, TipoAccion.PUNTUAL.codigo());
        assertEquals(2, TipoAccion.INICIO.codigo());
        assertEquals(3, TipoAccion.ACTUALIZACION.codigo());
        assertEquals(4, TipoAccion.FINALIZACION.codigo());
    }

    @Test
    void convierteCodigoValidoYRechazaDesconocidos() {
        assertEquals(TipoAccion.PUNTUAL,
                TipoAccion.desdeCodigo(1));
        assertEquals(TipoAccion.INICIO,
                TipoAccion.desdeCodigo(2));
        assertEquals(TipoAccion.ACTUALIZACION,
                TipoAccion.desdeCodigo(3));
        assertEquals(TipoAccion.FINALIZACION,
                TipoAccion.desdeCodigo(4));
        assertThrows(IllegalArgumentException.class,
                () -> TipoAccion.desdeCodigo(0));
        assertThrows(IllegalArgumentException.class,
                () -> TipoAccion.desdeCodigo(5));
    }

    @Test
    void codigosCabenenUnByte() {
        for (TipoAccion tipoAccion : TipoAccion.values()) {
            assertEquals(tipoAccion,
                    TipoAccion.desdeCodigo((byte) tipoAccion.codigo()));
        }
    }
}
