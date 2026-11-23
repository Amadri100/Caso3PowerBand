package powerband.controlador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import powerband.archivos.ValidacionesGenerales;
import powerband.comandos.BusComandos;
import powerband.excepciones.FalloTransporteException;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.FinalizadorGrabacion;
import powerband.grabaciones.GrabacionAbierta;
import powerband.grabaciones.Grabador;
import powerband.grabaciones.Reproductor;
import powerband.input.Entrada;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Configuraciones;

public final class Controlador implements FachadaControlador {

    private static final Controlador INSTANCIA =
            crearInstanciaProduccion();

    private final ControladorDispositivos dispositivos;
    private final ControladorGrabaciones grabaciones;
    private final List<ObservadorEstado> observadores = new ArrayList<>();
    private final Object bloqueoCicloVida = new Object();
    private EstadoControlador estado = EstadoControlador.INICIAL;
    private boolean conectando;
    private boolean grabando;
    private boolean reproduciendo;
    private volatile boolean desconectando;

    protected Controlador(
            BusComandos busComandos,
            Microfono microfono,
            Control control,
            Tableta tableta,
            EntradaMicrofono entradaMicrofono,
            EntradaControl entradaControl,
            EntradaTableta entradaTableta,
            Map<IdSintetizador, Sintetizador> sintetizadores,
            Grabador grabador,
            CargadorGrabaciones cargador,
            FinalizadorGrabacion finalizador,
            Reproductor reproductor,
            ArchivosGrabacion archivos) {
        this(
                busComandos,
                microfono,
                control,
                tableta,
                entradaMicrofono,
                entradaControl,
                entradaTableta,
                sintetizadores,
                grabador,
                cargador,
                finalizador,
                reproductor,
                archivos,
                EntradaControl::listarDispositivos);
    }

    protected Controlador(
            BusComandos busComandos,
            Microfono microfono,
            Control control,
            Tableta tableta,
            EntradaMicrofono entradaMicrofono,
            EntradaControl entradaControl,
            EntradaTableta entradaTableta,
            Map<IdSintetizador, Sintetizador> sintetizadores,
            Grabador grabador,
            CargadorGrabaciones cargador,
            FinalizadorGrabacion finalizador,
            Reproductor reproductor,
            ArchivosGrabacion archivos,
            Supplier<List<String>> proveedorDispositivos) {
        Objects.requireNonNull(busComandos, "busComandos");
        Objects.requireNonNull(microfono, "microfono");
        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(tableta, "tableta");
        if (microfono.busComandos() != busComandos
                || control.busComandos() != busComandos
                || tableta.busComandos() != busComandos) {
            throw new IllegalArgumentException(
                    "Todos los instrumentos deben compartir el BusComandos");
        }

        grabaciones = new ControladorGrabaciones(
                busComandos, grabador, cargador, finalizador, reproductor, archivos);
        dispositivos = new ControladorDispositivos(
                microfono, control, tableta, entradaMicrofono, entradaControl,
                entradaTableta, sintetizadores, proveedorDispositivos,
                this::notificarError, this::entradaDesconectada);
    }

    public static Controlador getInstance() {
        return INSTANCIA;
    }

    public static Controlador obtenerInstancia() {
        return getInstance();
    }

    public synchronized EstadoControlador estadoActual() {
        return estado;
    }

    @Override
    public boolean conectar() {
        if (desconectando) {
            return false;
        }
        synchronized (bloqueoCicloVida) {
            if (desconectando) {
                return false;
            }
            return conectarCoordinado();
        }
    }

    private boolean conectarCoordinado() {
        synchronized (this) {
            if (estado == EstadoControlador.CONECTADO) {
                return true;
            }
            if (conectando) {
                return false;
            }
            conectando = true;
        }
        cambiarEstado(EstadoControlador.CONECTANDO);

        try {
            dispositivos.iniciar();
            synchronized (this) {
                conectando = false;
                estado = EstadoControlador.CONECTADO;
            }
            cambiarEstado(EstadoControlador.CONECTADO);
            return true;
        } catch (RuntimeException | IOException excepcion) {
            synchronized (this) {
                conectando = false;
            }
            desconectarPorFallo(
                    "No se pudo iniciar la conexión", excepcion);
            return false;
        }
    }

    @Override
    public void desconectar() {
        synchronized (this) {
            if (desconectando) {
                return;
            }
            desconectando = true;
        }

        Throwable fallo = null;
        boolean estadoCambio = false;
        try {
            synchronized (bloqueoCicloVida) {
                if (!hayRecursosActivos()) {
                    return;
                }
                estadoCambio = estadoActual()
                        != EstadoControlador.INICIAL;
                fallo = desconectarRecursos(null);
            }
            if (estadoCambio) {
                cambiarEstado(EstadoControlador.INICIAL);
            }
            if (fallo != null) {
                notificarError(
                        "No se pudieron cerrar todos los recursos",
                        fallo, false);
                throw convertirEnRuntime(
                        "No se pudieron cerrar todos los recursos", fallo);
            }
        } finally {
            desconectando = false;
        }
    }

    @Override
    public List<String> listarDispositivos() {
        return dispositivos.listarDispositivos();
    }

    @Override
    public boolean asignarDispositivo(
            String instrumentoId, String dispositivoId) {
        return dispositivos.asignarDispositivo(instrumentoId, dispositivoId);
    }

    @Override
    public boolean asignarSintetizador(
            String instrumentoId, IdSintetizador sintetizadorId) {
        return dispositivos.asignarSintetizador(instrumentoId, sintetizadorId);
    }

    @Override
    public List<String> listarInstrumentos() {
        return dispositivos.listarInstrumentos();
    }

    @Override
    public List<IdSintetizador> listarSintetizadores() {
        return dispositivos.listarSintetizadores();
    }

    @Override
    public Map<String, String> dispositivoPorInstrumento() {
        return dispositivos.dispositivoPorInstrumento();
    }

    @Override
    public Map<String, List<IdSintetizador>>
            sintetizadoresPorInstrumento() {
        return dispositivos.sintetizadoresPorInstrumento();
    }

    @Override
    public boolean reemplazarSintetizadores(
            String instrumentoId,
            List<IdSintetizador> sintetizadorIds) {
        return dispositivos.reemplazarSintetizadores(instrumentoId, sintetizadorIds);
    }

    @Override
    public Map<String, Object> opcionesConfigurables() {
        return Configuraciones.getInstance().getOpcionesModificables();
    }

    @Override
    public Object consultarOpcion(String clave) {
        return Configuraciones.getInstance().getOpcion(clave);
    }

    @Override
    public void establecerOpcion(String clave, Object valor) {
        Configuraciones.getInstance().setOpcion(clave, valor);
    }

    @Override
    public void iniciarGrabacion() {
        if (desconectando) {
            throw new IllegalStateException(
                    "No se puede iniciar una grabación durante el cierre");
        }
        synchronized (bloqueoCicloVida) {
            if (desconectando) {
                throw new IllegalStateException(
                        "No se puede iniciar una grabación durante el cierre");
            }
            iniciarGrabacionCoordinada();
        }
    }

    private void iniciarGrabacionCoordinada() {
        synchronized (this) {
            if (estado != EstadoControlador.CONECTADO
                    || grabando || reproduciendo) {
                throw new IllegalStateException(
                        "No se puede iniciar la grabación en el estado actual");
            }
        }
        try {
            grabaciones.iniciar();
            synchronized (this) {
                grabando = true;
            }
            cambiarEstado(EstadoControlador.GRABANDO);
        } catch (IOException | RuntimeException excepcion) {
            notificarError("No se pudo iniciar la grabación", excepcion);
            throw excepcion instanceof RuntimeException runtime
                    ? runtime
                    : new IllegalStateException(
                            "No se pudo iniciar la grabación", excepcion);
        }
    }

    @Override
    public void detenerGrabacion(String nombre) {
        if (desconectando) {
            throw new IllegalStateException(
                    "No se puede finalizar la grabación durante el cierre");
        }
        synchronized (bloqueoCicloVida) {
            if (desconectando) {
                throw new IllegalStateException(
                        "No se puede finalizar la grabación durante el cierre");
            }
            detenerGrabacionCoordinada(nombre);
        }
    }

    private void detenerGrabacionCoordinada(String nombre) {
        synchronized (this) {
            if (!grabando) {
                throw new IllegalStateException(
                        "No hay una grabación activa");
            }
        }
        try {
            grabaciones.finalizar(nombre);
            synchronized (this) {
                grabando = false;
            }
            cambiarEstado(EstadoControlador.CONECTADO);
        } catch (IOException | RuntimeException excepcion) {
            synchronized (this) {
                grabando = false;
            }
            notificarError("No se pudo finalizar la grabación", excepcion);
            throw excepcion instanceof RuntimeException runtime
                    ? runtime
                    : new IllegalStateException(
                            "No se pudo finalizar la grabación", excepcion);
        }
    }

    @Override
    public List<String> listarSesiones() {
        try {
            return grabaciones.listarSesiones();
        } catch (IOException excepcion) {
            notificarError("No se pudieron listar las grabaciones", excepcion);
            throw new IllegalStateException(
                    "No se pudieron listar las grabaciones", excepcion);
        }
    }

    @Override
    public List<String> buscarSesiones(String nombre) {
        try {
            return grabaciones.buscarSesiones(nombre);
        } catch (IOException excepcion) {
            notificarError("No se pudieron listar las grabaciones", excepcion);
            throw new IllegalStateException(
                    "No se pudieron listar las grabaciones", excepcion);
        }
    }

    @Override
    public boolean reproducirSesion(String nombre) {
        if (desconectando) {
            return false;
        }
        synchronized (bloqueoCicloVida) {
            if (desconectando) {
                return false;
            }
            return reproducirSesionCoordinada(nombre);
        }
    }

    private boolean reproducirSesionCoordinada(String nombre) {
        ValidacionesGenerales.validarNombre(nombre);
        boolean estadoInvalido;
        synchronized (this) {
            estadoInvalido = estado != EstadoControlador.CONECTADO
                    || grabando || reproduciendo;
        }
        if (estadoInvalido) {
            notificarError(
                    "No se puede reproducir en el estado actual",
                    new IllegalStateException(
                            "Estado inválido para reproducir"));
            return false;
        }
        try {
            Optional<GrabacionAbierta> grabacion = grabaciones.abrir(nombre);
            if (grabacion.isEmpty()) {
                notificarError(
                        "La sesión no existe: " + nombre,
                        new IllegalArgumentException(
                                "La sesión no existe"));
                return false;
            }
            synchronized (this) {
                reproduciendo = true;
            }
            cambiarEstado(EstadoControlador.REPRODUCIENDO);
            grabaciones.iniciarReproduccion(
                    nombre, grabacion.orElseThrow(), this::reproduccionFinalizada);
            return true;
        } catch (IOException | RuntimeException excepcion) {
            synchronized (this) {
                reproduciendo = false;
            }
            notificarError("No se pudo reproducir la sesión", excepcion);
            return false;
        }
    }

    @Override
    public void registrarObservadorEstado(ObservadorEstado observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            boolean registrado = observadores.stream()
                    .anyMatch(actual -> actual == observador);
            if (!registrado) {
                observadores.add(observador);
            }
        }
    }

    @Override
    public void retirarObservadorEstado(ObservadorEstado observador) {
        Objects.requireNonNull(observador, "observador");
        synchronized (observadores) {
            observadores.removeIf(actual -> actual == observador);
        }
    }

    private void reproduccionFinalizada(Throwable excepcion) {
        boolean falloGlobal = false;
        try {
            if (excepcion == null) {
                return;
            }
            if (esFalloTransporte(excepcion)) {
                falloGlobal = true;
                desconectarPorFallo(
                        "Fallo local de transporte durante la reproducción",
                        excepcion, false);
            } else {
                notificarError(
                        "Error durante la reproducción", excepcion);
            }
        } finally {
            boolean notificarEstado;
            synchronized (this) {
                reproduciendo = false;
                grabaciones.finalizarReproduccion();
                notificarEstado = !desconectando && !falloGlobal;
                if (notificarEstado) {
                    estado = EstadoControlador.CONECTADO;
                }
            }
            if (notificarEstado) {
                cambiarEstado(EstadoControlador.CONECTADO);
            }
        }
    }

    private void entradaDesconectada(
            Entrada entrada, String motivo, Throwable causa) {
        String nombre = entrada == null ? "desconocida" : entrada.nombre();
        String mensaje = "Desconexión de entrada " + nombre
                + ": " + (motivo == null ? "dispositivo desconectado"
                        : motivo);
        if (desconectando) {
            Throwable causaNotificacion = causa == null
                    ? new IllegalStateException(mensaje)
                    : causa;
            notificarError(mensaje, causaNotificacion, false);
            return;
        }
        if (esFalloTransporte(causa)) {
            desconectarPorFallo(mensaje, causa, false);
            return;
        }
        if (entrada != null) {
            dispositivos.retirarDispositivo(nombre);
        }
        Throwable causaNotificacion = causa == null
                ? new IllegalStateException(mensaje)
                : causa;
        notificarError(mensaje, causaNotificacion, false);
    }

    private void desconectarPorFallo(
            String mensaje, Throwable causa) {
        desconectarPorFallo(mensaje, causa, true);
    }

    private void desconectarPorFallo(
            String mensaje, Throwable causa, boolean registrar) {
        Throwable causaPrincipal = causa == null
                ? new IllegalStateException(mensaje)
                : causa;
        boolean cierreEnCurso;
        synchronized (this) {
            cierreEnCurso = desconectando;
            if (!cierreEnCurso) {
                desconectando = true;
            }
        }
        if (cierreEnCurso) {
            notificarError(mensaje, causaPrincipal, registrar);
            return;
        }

        Throwable falloFinal;
        boolean estadoCambio;
        try {
            synchronized (bloqueoCicloVida) {
                estadoCambio = estadoActual()
                        != EstadoControlador.INICIAL;
                falloFinal = hayRecursosActivos()
                        ? desconectarRecursos(causaPrincipal)
                        : causaPrincipal;
            }
            if (estadoCambio) {
                cambiarEstado(EstadoControlador.INICIAL);
            }
            notificarError(mensaje, falloFinal, registrar);
        } finally {
            desconectando = false;
        }
    }

    private Throwable desconectarRecursos(Throwable falloPrincipal) {
        Throwable fallo = dispositivos.detenerEntradas(falloPrincipal);
        fallo = grabaciones.abortarGrabacion(fallo, grabando);
        fallo = grabaciones.detenerReproduccion(fallo);
        fallo = dispositivos.liberar(fallo);
        synchronized (this) {
            grabando = false;
            reproduciendo = false;
            estado = EstadoControlador.INICIAL;
        }
        return fallo;
    }

    private boolean hayRecursosActivos() {
        synchronized (this) {
            if (estado != EstadoControlador.INICIAL
                    || conectando || grabando
                    || reproduciendo || grabaciones.hayReproduccion()) {
                return true;
            }
        }
        return grabaciones.estaGrabando() || dispositivos.hayRecursosActivos();
    }

    private static RuntimeException convertirEnRuntime(
            String mensaje, Throwable causa) {
        return causa instanceof RuntimeException runtime
                ? runtime
                : new IllegalStateException(mensaje, causa);
    }

    private static boolean esFalloTransporte(Throwable causa) {
        Throwable actual = causa;
        while (actual != null) {
            if (actual instanceof FalloTransporteException) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    private void cambiarEstado(EstadoControlador nuevoEstado) {
        synchronized (this) {
            estado = nuevoEstado;
        }
        List<ObservadorEstado> copia;
        synchronized (observadores) {
            copia = List.copyOf(observadores);
        }
        for (ObservadorEstado observador : copia) {
            try {
                observador.estadoCambiado(nuevoEstado);
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarError(
                        "Fallo al notificar un estado a un observador: "
                                + excepcion);
            }
        }
    }

    private void notificarError(String mensaje, Throwable causa) {
        notificarError(mensaje, causa, true);
    }

    private void notificarError(
            String mensaje, Throwable causa, boolean registrar) {
        if (registrar) {
            LoggerPowerBand.getInstance().registrarError(
                    mensaje + ": " + causa);
        }
        List<ObservadorEstado> copia;
        synchronized (observadores) {
            copia = List.copyOf(observadores);
        }
        for (ObservadorEstado observador : copia) {
            try {
                observador.errorNotificado(mensaje, causa);
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarError(
                        "Fallo al notificar un error a un observador: "
                                + excepcion);
            }
        }
    }

    private static Controlador crearInstanciaProduccion() {
        try {
            return FabricaControlador.crear();
        } catch (RuntimeException | IOException excepcion) {
            throw new ExceptionInInitializerError(excepcion);
        }
    }
}
