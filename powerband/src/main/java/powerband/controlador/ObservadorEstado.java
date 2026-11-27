package powerband.controlador;

public interface ObservadorEstado {

    public void estadoCambiado(EstadoControlador estado);

    public void errorNotificado(String mensaje, Throwable causa);

    //Default permite agregar una implementación.
    public default void dispositivoDesconectado(String instrumentoId) {
        
    }
}
