package powerband.input;

@FunctionalInterface
public interface ObservadorDesconexion {

    void entradaDesconectada(Entrada entrada, String motivo);
}
