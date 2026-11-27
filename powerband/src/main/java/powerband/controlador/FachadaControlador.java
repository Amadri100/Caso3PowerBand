package powerband.controlador;

import java.awt.Component;
import java.util.List;
import java.util.Map;

import powerband.util.DispositivoEntrada;
import powerband.sintetizadores.IdSintetizador;

public interface FachadaControlador {

    public boolean conectar();

    public void desconectar();

    public List<String> listarDispositivos();

    public List<DispositivoEntrada> listarDispositivos(String instrumentoId);

    public Component superficieTableta();

    public void establecerModoDescubrimientoTableta(boolean activo);

    public boolean asignarDispositivo(String instrumentoId, String dispositivoId);

    public boolean asignarSintetizador(
            String instrumentoId,
            IdSintetizador sintetizadorId);

    public List<String> listarInstrumentos();

    public List<IdSintetizador> listarSintetizadores();

    public Map<String, String> dispositivoPorInstrumento();

    public Map<String, List<IdSintetizador>> sintetizadoresPorInstrumento();

    public boolean reemplazarSintetizadores(
            String instrumentoId,
            List<IdSintetizador> sintetizadorIds);

    public Map<String, Object> opcionesConfigurables();

    public Object consultarOpcion(String clave);

    public void establecerOpcion(String clave, Object valor);

    public void iniciarGrabacion();

    public void detenerGrabacion(String nombre);

    public List<String> listarSesiones();

    public List<String> buscarSesiones(String nombre);

    public boolean reproducirSesion(String nombre);

    public void registrarObservadorEstado(ObservadorEstado observador);

    public void retirarObservadorEstado(ObservadorEstado observador);
}
