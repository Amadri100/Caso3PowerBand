package powerband.comandos;

@FunctionalInterface
public interface ObservadorComandos {

    void comandoEnviado(Comando comando, long momentoNanos);
}
