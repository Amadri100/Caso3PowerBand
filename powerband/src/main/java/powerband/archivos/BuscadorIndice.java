package powerband.archivos;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import powerband.util.Constantes;

public final class BuscadorIndice {

    private final ArchivoRegistros<EntradaIndice> archivo;

    public BuscadorIndice(Path rutaIndice) {
        Objects.requireNonNull(rutaIndice, "rutaIndice");
        this.archivo = new ArchivoRegistros<>(rutaIndice,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes, EntradaIndice::serializar);
    }

    public Optional<EntradaIndice> buscar(String nombre) throws IOException {
        ValidacionesGenerales.validarNombre(nombre);
        //Se utilizan los siguientes, 
        long inferior = 0;
        long superior = archivo.cantidadRegistros() - 1; 
        //un punto medio entre el inferior y el superior
        while (inferior <= superior) {
            long medio = inferior + (superior - inferior) / 2; 
            //linea -> pos en bytes
            long posicionBytes =
                    medio * Constantes.TAMANO_ENTRADA_INDICE;
            EntradaIndice entrada = archivo.leer(posicionBytes);
            int comparacion = ComparadorNombres.INSTANCIA.compare(
                    entrada.nombre(), nombre);
            //Iguales entonces se encuentra
            if (comparacion == 0) {
                return Optional.of(entrada);
            }
            // Si es mayor tomamos "descartamos" la mitad inferior de la lista
            if (comparacion < 0) {
                inferior = medio + 1;
            }
            // Si es menor descartamos la parte superior 
            else {
                superior = medio - 1;
            }
        }
        return Optional.empty();
    }
}
