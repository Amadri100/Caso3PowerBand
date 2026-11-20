package powerband.archivos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import powerband.util.Constantes;

public final class BuscadorIndice {

    private final Path rutaIndice;

    public BuscadorIndice(Path rutaIndice) {
        this.rutaIndice = Objects.requireNonNull(rutaIndice, "rutaIndice");
    }

    public Optional<EntradaIndice> buscar(String nombre) throws IOException {
        ValidacionesGenerales.validarNombre(nombre);
        try (ArchivoRegistros<EntradaIndice> archivo =
                new ArchivoRegistros<>(rutaIndice,
                    Constantes.TAMANO_ENTRADA_INDICE,
                    EntradaIndice::desdeBytes, EntradaIndice::serializar)) {
            long inferior = 0;
            long superior = archivo.cantidadRegistros() - 1;
            while (inferior <= superior) {
                long medio = inferior + (superior - inferior) / 2;
                long posicionBytes =
                        medio * Constantes.TAMANO_ENTRADA_INDICE;
                EntradaIndice entrada = archivo.leer(posicionBytes);
                int comparacion = ComparadorNombres.INSTANCIA.compare(
                        entrada.nombre(), nombre);
                if (comparacion == 0) {
                    return Optional.of(entrada);
                }
                if (comparacion < 0) {
                    inferior = medio + 1;
                } else {
                    superior = medio - 1;
                }
            }
            return Optional.empty();
        }
    }
}
