package powerband.archivos;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComparadorNombresTest {

    @Test
    void comparaSinDistinguirMayusculasYOrdenaConElMismoCriterio() {
        assertEquals(0,
                ComparadorNombres.INSTANCIA.compare("Piano", "pIaNo"));

        List<String> nombres = new ArrayList<>(
                List.of("zeta", "ALFA", "Beta"));
        nombres.sort(ComparadorNombres.INSTANCIA);

        assertEquals(List.of("ALFA", "Beta", "zeta"), nombres);
    }

    @Test
    void comparacionNoDependeDelLocaleYRechazaNulos() {
        Locale localeAnterior = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(0,
                    ComparadorNombres.INSTANCIA.compare("I", "i"));
        } finally {
            Locale.setDefault(localeAnterior);
        }

        assertThrows(NullPointerException.class,
                () -> ComparadorNombres.INSTANCIA.compare(null, "nombre"));
        assertThrows(NullPointerException.class,
                () -> ComparadorNombres.INSTANCIA.compare("nombre", null));
    }
}
