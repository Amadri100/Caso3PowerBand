package powerband.sintetizadores;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdSintetizadorTest {

    @Test
    void exponeCodigosPersistentesEstables() {
        assertEquals(3, IdSintetizador.values().length);
        assertEquals(1, IdSintetizador.PIANO.codigo());
        assertEquals(2, IdSintetizador.BAJO.codigo());
        assertEquals(3, IdSintetizador.ORGANO.codigo());
    }

    @Test
    void convierteCodigoValidoYRechazaDesconocidos() {
        assertEquals(IdSintetizador.PIANO,
                IdSintetizador.desdeCodigo(1));
        assertEquals(IdSintetizador.BAJO,
                IdSintetizador.desdeCodigo(2));
        assertEquals(IdSintetizador.ORGANO,
                IdSintetizador.desdeCodigo(3));
        assertThrows(IllegalArgumentException.class,
                () -> IdSintetizador.desdeCodigo(0));
        assertThrows(IllegalArgumentException.class,
                () -> IdSintetizador.desdeCodigo(4));
    }

    @Test
    void codigosCabenenUnByte() {
        for (IdSintetizador sintetizador : IdSintetizador.values()) {
            assertEquals(sintetizador,
                    IdSintetizador.desdeCodigo((byte) sintetizador.codigo()));
        }
    }
}
