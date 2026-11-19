package powerband.archivos;

import java.util.Comparator;
import java.util.Objects;

public final class ComparadorNombres implements Comparator<String> {

    public static final ComparadorNombres INSTANCIA =
            new ComparadorNombres();

    private ComparadorNombres() {
    }

    @Override
    public int compare(String nombre1, String nombre2) {
        return String.CASE_INSENSITIVE_ORDER.compare(
                Objects.requireNonNull(nombre1, "nombre1"),
                Objects.requireNonNull(nombre2, "nombre2"));
    }
}
