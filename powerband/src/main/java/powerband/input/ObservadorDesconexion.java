package powerband.input;

@FunctionalInterface
public interface ObservadorDesconexion {

    public void entradaDesconectada(
            Entrada entrada, String motivo, Throwable causa);
}
