package powerband.util;

import java.util.Objects;

public record DispositivoEntrada(String id, String nombre) {

    public DispositivoEntrada {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        if (id.isBlank() || nombre.isBlank()) {
            throw new IllegalArgumentException("El dispositivo debe tener identificador y nombre");
        }
    }

    @Override
    public String toString() {
        return nombre;
    }
}
