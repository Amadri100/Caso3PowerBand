package powerband.controlador;

import java.awt.Canvas;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import powerband.archivos.ValidacionesGenerales;
import powerband.util.Configuraciones;
import powerband.util.Constantes;
import powerband.util.DispositivoEntrada;
import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.sintetizadores.IdSintetizador;

public final class FachadaControladorFalsa implements FachadaControlador {

    private static final List<String> DISPOSITIVOS_EJEMPLO = List.of(
            "audio-demo", "gamepad-demo", "lapiz-demo");
    private static final List<String> INSTRUMENTOS_EJEMPLO = List.of(
            "microfono", "control", "tableta");
    private static final Map<String, String> DISPOSITIVO_POR_INSTRUMENTO = Map.of(
            "microfono", "audio-demo", "control", "gamepad-demo", "tableta", "lapiz-demo");

    private final Component superficie = new Canvas();
    private final List<ObservadorEstado> observadores = new ArrayList<>();
    private final Map<String, String> asignacionesDispositivo =
            new LinkedHashMap<>();
    private final Map<String, List<IdSintetizador>> asignacionesSintetizador =
            new LinkedHashMap<>();
    private final Map<String, Object> opciones = new LinkedHashMap<>();
    private final List<String> sesiones = new ArrayList<>(List.of(
            "Demo Piano", "Demo Organo"));

    private EstadoControlador estado = EstadoControlador.INICIAL;
    private boolean conectado;
    private boolean grabando;
    private boolean reproduciendo;

    @Override
    public boolean conectar() {
        if (conectado) {
            return true;
        }
        conectado = true;
        cambiarEstado(EstadoControlador.CONECTANDO);
        cambiarEstado(EstadoControlador.CONECTADO);
        return true;
    }

    @Override
    public void desconectar() {
        if (!conectado && !grabando && !reproduciendo) {
            return;
        }
        conectado = false;
        grabando = false;
        reproduciendo = false;
        cambiarEstado(EstadoControlador.INICIAL);
    }

    @Override
    public List<String> listarDispositivos() {
        return List.copyOf(DISPOSITIVOS_EJEMPLO);
    }

    @Override
    public List<DispositivoEntrada> listarDispositivos(String instrumentoId) {
        String dispositivo = instrumentoId == null ? null
                : DISPOSITIVO_POR_INSTRUMENTO.get(instrumentoId);
        if (dispositivo == null) {
            throw errorArgumento("El instrumento de ejemplo no existe");
        }
        return List.of(new DispositivoEntrada(dispositivo, dispositivo));
    }

    @Override
    public Component superficieTableta() {
        return superficie;
    }

    @Override
    public void establecerModoDescubrimientoTableta(boolean activo) {
    }

    @Override
    public boolean asignarDispositivo(
            String instrumentoId, String dispositivoId) {
        if (instrumentoId == null || !INSTRUMENTOS_EJEMPLO.contains(instrumentoId)
                || !Objects.equals(DISPOSITIVO_POR_INSTRUMENTO.get(instrumentoId), dispositivoId)
                || asignacionesDispositivo.entrySet().stream()
                        .anyMatch(asignacion -> !asignacion.getKey().equals(instrumentoId)
                                && asignacion.getValue().equals(dispositivoId))) {
            IllegalArgumentException error = new IllegalArgumentException(
                    "El instrumento o dispositivo de ejemplo no existe");
            notificarError(error.getMessage(), error);
            return false;
        }
        asignacionesDispositivo.put(instrumentoId, dispositivoId);
        return true;
    }

    @Override
    public boolean asignarSintetizador(
            String instrumentoId, IdSintetizador sintetizadorId) {
        if (!INSTRUMENTOS_EJEMPLO.contains(instrumentoId)
                || sintetizadorId == null) {
            IllegalArgumentException error = new IllegalArgumentException(
                    "El instrumento o sintetizador de ejemplo no es válido");
            notificarError(error.getMessage(), error);
            return false;
        }
        asignacionesSintetizador.put(
                instrumentoId, List.of(sintetizadorId));
        return true;
    }

    @Override
    public List<String> listarInstrumentos() {
        return List.copyOf(INSTRUMENTOS_EJEMPLO);
    }

    @Override
    public List<IdSintetizador> listarSintetizadores() {
        return List.of(
                IdSintetizador.PIANO,
                IdSintetizador.BAJO,
                IdSintetizador.ORGANO);
    }

    @Override
    public Map<String, String> dispositivoPorInstrumento() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(asignacionesDispositivo));
    }

    @Override
    public Map<String, List<IdSintetizador>>
            sintetizadoresPorInstrumento() {
        Map<String, List<IdSintetizador>> copia =
                new LinkedHashMap<>();
        for (Map.Entry<String, List<IdSintetizador>> asignacion
                : asignacionesSintetizador.entrySet()) {
            copia.put(asignacion.getKey(),
                    List.copyOf(asignacion.getValue()));
        }
        return Collections.unmodifiableMap(copia);
    }

    @Override
    public boolean reemplazarSintetizadores(
            String instrumentoId,
            List<IdSintetizador> sintetizadorIds) {
        Objects.requireNonNull(
                sintetizadorIds, "sintetizadorIds");
        if (!INSTRUMENTOS_EJEMPLO.contains(instrumentoId)) {
            notificarError(
                    "El instrumento de ejemplo no existe",
                    new IllegalArgumentException(
                            "El instrumento de ejemplo no existe"));
            return false;
        }
        for (IdSintetizador sintetizadorId : sintetizadorIds) {
            if (sintetizadorId == null
                    || !List.of(
                            IdSintetizador.PIANO,
                            IdSintetizador.BAJO,
                            IdSintetizador.ORGANO)
                            .contains(sintetizadorId)) {
                notificarError(
                        "El sintetizador de ejemplo no es válido",
                        new IllegalArgumentException(
                                "El sintetizador de ejemplo no es válido"));
                return false;
            }
        }
        asignacionesSintetizador.put(
                instrumentoId, List.copyOf(sintetizadorIds));
        return true;
    }

    @Override
    public Map<String, Object> opcionesConfigurables() {
        opciones.put(Constantes.OPCION_DEBUG_LOG,
                Configuraciones.getInstance().getOpcion(Constantes.OPCION_DEBUG_LOG));
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(opciones));
    }

    @Override
    public Object consultarOpcion(String clave) {
        validarClaveOpcion(clave);
        if (Constantes.OPCION_DEBUG_LOG.equals(clave)) {
            return Configuraciones.getInstance().getOpcion(clave);
        }
        if (!opciones.containsKey(clave)) {
            throw errorArgumento("La opción configurable no existe: " + clave);
        }
        return opciones.get(clave);
    }

    @Override
    public void establecerOpcion(String clave, Object valor) {
        validarClaveOpcion(clave);
        if (Constantes.OPCION_DEBUG_LOG.equals(clave)) {
            Configuraciones.getInstance().setOpcion(clave, valor);
            return;
        }
        if (!opciones.containsKey(clave)) {
            throw errorArgumento("La opción configurable no existe: " + clave);
        }
        opciones.put(clave, valor);
    }

    @Override
    public void iniciarGrabacion() {
        if (!conectado || grabando || reproduciendo) {
            throw errorEstado(
                    "No se puede iniciar la grabación en el estado actual");
        }
        grabando = true;
        cambiarEstado(EstadoControlador.GRABANDO);
    }

    @Override
    public void detenerGrabacion(String nombre) {
        if (!grabando) {
            throw errorEstado("No hay una grabación de ejemplo activa");
        }
        try {
            ValidacionesGenerales.validarNombre(nombre);
        } catch (IllegalArgumentException error) {
            notificarError(error.getMessage(), error);
            throw error;
        }
        boolean duplicado = sesiones.stream()
                .anyMatch(sesion -> sesion.equalsIgnoreCase(nombre));
        if (duplicado) {
            throw errorArgumento("Ya existe la sesión de ejemplo: " + nombre);
        }
        sesiones.add(nombre);
        grabando = false;
        cambiarEstado(EstadoControlador.CONECTADO);
    }

    @Override
    public List<String> listarSesiones() {
        return List.copyOf(sesiones);
    }

    @Override
    public List<String> buscarSesiones(String nombre) {
        try {
            ValidacionesGenerales.validarNombre(nombre);
        } catch (IllegalArgumentException error) {
            notificarError(error.getMessage(), error);
            throw error;
        }
        return sesiones.stream()
                .filter(sesion -> sesion.equalsIgnoreCase(nombre))
                .toList();
    }

    @Override
    public boolean reproducirSesion(String nombre) {
        try {
            ValidacionesGenerales.validarNombre(nombre);
        } catch (IllegalArgumentException error) {
            notificarError(error.getMessage(), error);
            throw error;
        }
        if (!conectado || grabando || reproduciendo) {
            throw errorEstado(
                    "No se puede reproducir una sesión en el estado actual");
        }
        boolean existe = sesiones.stream()
                .anyMatch(sesion -> sesion.equalsIgnoreCase(nombre));
        if (!existe) {
            IllegalArgumentException error = new IllegalArgumentException(
                    "La sesión de ejemplo no existe: " + nombre);
            notificarError(error.getMessage(), error);
            return false;
        }

        reproduciendo = true;
        cambiarEstado(EstadoControlador.REPRODUCIENDO);
        reproduciendo = false;
        cambiarEstado(EstadoControlador.CONECTADO);
        return true;
    }

    @Override
    public void registrarObservadorEstado(ObservadorEstado observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            boolean yaRegistrado = observadores.stream()
                    .anyMatch(registrado -> registrado == observador);
            if (!yaRegistrado) {
                observadores.add(observador);
            }
        }
    }

    @Override
    public void retirarObservadorEstado(ObservadorEstado observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            observadores.removeIf(registrado -> registrado == observador);
        }
    }

    private void cambiarEstado(EstadoControlador nuevoEstado) {
        if (estado == nuevoEstado) {
            return;
        }
        estado = nuevoEstado;
        List<ObservadorEstado> copiaObservadores;
        synchronized (observadores) {
            copiaObservadores = List.copyOf(observadores);
        }

        List<RuntimeException> fallos = new ArrayList<>();
        for (ObservadorEstado observador : copiaObservadores) {
            try {
                observador.estadoCambiado(nuevoEstado);
            } catch (RuntimeException fallo) {
                fallos.add(fallo);
            }
        }
        registrarFallosObservadores("estado", fallos);
    }

    private void notificarError(String mensaje, Throwable causa) {
        List<ObservadorEstado> copiaObservadores;
        synchronized (observadores) {
            copiaObservadores = List.copyOf(observadores);
        }

        List<RuntimeException> fallos = new ArrayList<>();
        for (ObservadorEstado observador : copiaObservadores) {
            try {
                observador.errorNotificado(mensaje, causa);
            } catch (RuntimeException fallo) {
                fallos.add(fallo);
            }
        }
        LoggerPowerBand.getInstance().registrar(Log.error(
                mensaje + (causa == null ? "" : ": " + causa),
                OrigenLog.CONTROLADOR, causa));
        registrarFallosObservadores("error", fallos);
    }

    private void registrarFallosObservadores(
            String tipoNotificacion, List<RuntimeException> fallos) {
        for (RuntimeException fallo : fallos) {
            LoggerPowerBand.getInstance().registrar(Log.error(
                    "Fallo al notificar " + tipoNotificacion
                            + " a un observador: " + fallo, OrigenLog.CONTROLADOR, fallo));
        }
    }

    private IllegalArgumentException errorArgumento(String mensaje) {
        IllegalArgumentException error = new IllegalArgumentException(mensaje);
        notificarError(mensaje, error);
        return error;
    }

    private IllegalStateException errorEstado(String mensaje) {
        IllegalStateException error = new IllegalStateException(mensaje);
        notificarError(mensaje, error);
        return error;
    }

    private void validarClaveOpcion(String clave) {
        if (clave == null || clave.isBlank()) {
            throw errorArgumento("La clave de opción no puede estar vacía");
        }
    }
}
