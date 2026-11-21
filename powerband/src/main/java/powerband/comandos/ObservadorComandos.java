package powerband.comandos;

@FunctionalInterface
public interface ObservadorComandos {

    boolean comandoEnviado(Comando comando, long momentoNanos);
}
