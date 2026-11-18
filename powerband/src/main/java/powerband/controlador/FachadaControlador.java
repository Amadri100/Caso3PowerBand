package powerband.controlador;

import java.util.List;
import java.util.Map;

import powerband.sintetizadores.IdSintetizador;

public interface FachadaControlador {

    boolean conectar();

    void desconectar();

    List<String> listarDispositivos();

    boolean asignarDispositivo(String instrumentoId, String dispositivoId);

    boolean asignarSintetizador(
            String instrumentoId,
            IdSintetizador sintetizadorId);

    Map<String, Object> opcionesConfigurables();

    Object consultarOpcion(String clave);

    void establecerOpcion(String clave, Object valor);

    void iniciarGrabacion();

    void detenerGrabacion(String nombre);

    List<String> listarSesiones();

    List<String> buscarSesiones(String nombre);

    boolean reproducirSesion(String nombre);

    void registrarObservadorEstado(ObservadorEstado observador);

    void retirarObservadorEstado(ObservadorEstado observador);
}
