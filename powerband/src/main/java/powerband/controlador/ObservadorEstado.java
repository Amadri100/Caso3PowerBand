package powerband.controlador;

public interface ObservadorEstado {

    void estadoCambiado(EstadoControlador estado);

    void errorNotificado(String mensaje, Throwable causa);
}
