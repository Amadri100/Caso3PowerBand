package powerband.controlador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.LockSupport;

import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ValidacionesGenerales;
import powerband.comandos.BusComandos;
import powerband.comandos.FabricaComandosGrabados;
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
import powerband.input.ObservadorDesconexion;
import powerband.instrumentos.Control;
import powerband.instrumentos.Instrumento;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.BandClient;
import powerband.sintetizadores.BajoSintetizador;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.OrganoSintetizador;
import powerband.sintetizadores.PianoSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Configuraciones;

public final class Controlador implements FachadaControlador {

    private static final Controlador INSTANCIA =
            crearInstanciaProduccion();

    private static final List<String> INSTRUMENTOS = List.of(
            "microfono", "control", "tableta");

    private final BusComandos busComandos;
    private final Microfono microfono;
    private final Control control;
    private final Tableta tableta;
    private final EntradaMicrofono entradaMicrofono;
    private final EntradaControl entradaControl;
    private final EntradaTableta entradaTableta;
    private final Map<String, Instrumento> instrumentos;
    private final Map<IdSintetizador, Sintetizador> sintetizadores;
    private final Map<String, String> asignacionesDispositivo =
            new LinkedHashMap<>();
    private final Map<String, List<IdSintetizador>> asignacionesSintetizador =
            new LinkedHashMap<>();
    private final List<ObservadorEstado> observadores = new ArrayList<>();
    private final Grabador grabador;
    private final CargadorGrabaciones cargador;
    private final FinalizadorGrabacion finalizador;
    private final Reproductor reproductor;
    private final ArchivosGrabacion archivos;

    private EstadoControlador estado = EstadoControlador.INICIAL;
    private BandClient bandClient;
    private volatile Thread hiloReproduccion;
    private boolean conectando;
    private boolean grabando;
    private boolean reproduciendo;

    private Controlador() throws IOException {
        this(crearComponentesProduccion());
    }

    private Controlador(ComponentesProduccion componentes) {
        this(componentes.busComandos,
                componentes.microfono,
                componentes.control,
                componentes.tableta,
                componentes.entradaMicrofono,
                componentes.entradaControl,
                componentes.entradaTableta,
                componentes.sintetizadores,
                componentes.grabador,
                componentes.cargador,
                componentes.finalizador,
                componentes.reproductor,
                componentes.archivos);
    }

    Controlador(
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
        this.busComandos = Objects.requireNonNull(
                busComandos, "busComandos");
        this.microfono = Objects.requireNonNull(
                microfono, "microfono");
        this.control = Objects.requireNonNull(control, "control");
        this.tableta = Objects.requireNonNull(tableta, "tableta");
        this.entradaMicrofono = Objects.requireNonNull(
                entradaMicrofono, "entradaMicrofono");
        this.entradaControl = Objects.requireNonNull(
                entradaControl, "entradaControl");
        this.entradaTableta = Objects.requireNonNull(
                entradaTableta, "entradaTableta");
        this.sintetizadores = new LinkedHashMap<>(
                Objects.requireNonNull(
                        sintetizadores, "sintetizadores"));
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.cargador = Objects.requireNonNull(cargador, "cargador");
        this.finalizador = Objects.requireNonNull(
                finalizador, "finalizador");
        this.reproductor = Objects.requireNonNull(
                reproductor, "reproductor");
        this.archivos = archivos;

        instrumentos = new LinkedHashMap<>();
        instrumentos.put("microfono", microfono);
        instrumentos.put("control", control);
        instrumentos.put("tableta", tableta);

        ObservadorDesconexion observador = (entrada, motivo) ->
                entradaDesconectada(entrada, motivo);
        entradaMicrofono.registrarObservadorDesconexion(observador);
        entradaControl.registrarObservadorDesconexion(observador);
        entradaTableta.registrarObservadorDesconexion(observador);
    }

    public static Controlador getInstance() {
        return INSTANCIA;
    }

    public static Controlador obtenerInstancia() {
        return getInstance();
    }

    public EstadoControlador estadoActual() {
        return estado;
    }

    @Override
    public boolean conectar() {
        synchronized (this) {
            if (estado == EstadoControlador.CONECTADO) {
                return true;
            }
            if (conectando) {
                return false;
            }
            conectando = true;
            cambiarEstado(EstadoControlador.CONECTANDO);
        }

        try {
            asegurarSintetizadores();
            entradaMicrofono.iniciar();
            entradaControl.iniciar();
            entradaTableta.iniciar();
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
        desconectarRecursos();
        cambiarEstado(EstadoControlador.INICIAL);
    }

    @Override
    public List<String> listarDispositivos() {
        return EntradaControl.listarDispositivos();
    }

    @Override
    public boolean asignarDispositivo(
            String instrumentoId, String dispositivoId) {
        if (!INSTRUMENTOS.contains(instrumentoId)
                || !EntradaControl.esDispositivoValido(dispositivoId)) {
            IllegalArgumentException error = new IllegalArgumentException(
                    "Instrumento o dispositivo no válido");
            notificarError(error.getMessage(), error);
            return false;
        }
        if (!entradaControl.estaActiva() && "control".equals(instrumentoId)) {
            entradaControl.seleccionarDispositivo(dispositivoId);
        } else if ("control".equals(instrumentoId)) {
            entradaControl.seleccionarDispositivo(dispositivoId);
        }
        asignacionesDispositivo.put(instrumentoId, dispositivoId);
        return true;
    }

    @Override
    public boolean asignarSintetizador(
            String instrumentoId, IdSintetizador sintetizadorId) {
        Objects.requireNonNull(sintetizadorId, "sintetizadorId");
        Instrumento instrumento = instrumentos.get(instrumentoId);
        Sintetizador sintetizador = sintetizadores.get(sintetizadorId);
        if (instrumento == null || sintetizador == null) {
            IllegalArgumentException error = new IllegalArgumentException(
                    "Instrumento o sintetizador no válido");
            notificarError(error.getMessage(), error);
            return false;
        }
        instrumento.reemplazarSintetizador(sintetizador);
        asignacionesSintetizador.put(
                instrumentoId, List.of(sintetizadorId));
        return true;
    }

    @Override
    public List<String> listarInstrumentos() {
        return INSTRUMENTOS;
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
        Objects.requireNonNull(sintetizadorIds, "sintetizadorIds");
        Instrumento instrumento = instrumentos.get(instrumentoId);
        if (instrumento == null) {
            notificarError(
                    "Instrumento no válido",
                    new IllegalArgumentException(
                            "Instrumento no válido"));
            return false;
        }
        List<Sintetizador> destinos = new ArrayList<>();
        for (IdSintetizador sintetizadorId : sintetizadorIds) {
            Sintetizador sintetizador = sintetizadores.get(sintetizadorId);
            if (sintetizador == null) {
                notificarError(
                        "Sintetizador no válido",
                        new IllegalArgumentException(
                                "Sintetizador no válido"));
                return false;
            }
            destinos.add(sintetizador);
        }
        instrumento.reemplazarSintetizadores(destinos);
        asignacionesSintetizador.put(
                instrumentoId, List.copyOf(sintetizadorIds));
        return true;
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
        synchronized (this) {
            if (estado != EstadoControlador.CONECTADO
                    || grabando || reproduciendo) {
                throw new IllegalStateException(
                        "No se puede iniciar la grabación en el estado actual");
            }
        }
        try {
            grabador.iniciar();
            busComandos.registrarObservador(grabador);
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
        synchronized (this) {
            if (!grabando) {
                throw new IllegalStateException(
                        "No hay una grabación activa");
            }
        }
        try {
            finalizador.finalizar(nombre);
            busComandos.quitarObservador(grabador);
            synchronized (this) {
                grabando = false;
            }
            cambiarEstado(EstadoControlador.CONECTADO);
        } catch (IOException | RuntimeException excepcion) {
            synchronized (this) {
                grabando = false;
            }
            busComandos.quitarObservador(grabador);
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
            List<String> nombres = new ArrayList<>();
            for (EntradaListaGrabacion entrada : cargador.listar()) {
                nombres.add(entrada.nombre());
            }
            return List.copyOf(nombres);
        } catch (IOException excepcion) {
            notificarError("No se pudieron listar las grabaciones", excepcion);
            throw new IllegalStateException(
                    "No se pudieron listar las grabaciones", excepcion);
        }
    }

    @Override
    public List<String> buscarSesiones(String nombre) {
        ValidacionesGenerales.validarNombre(nombre);
        String filtro = nombre.toLowerCase(Locale.ROOT);
        List<String> coincidencias = new ArrayList<>();
        for (String sesion : listarSesiones()) {
            if (sesion.toLowerCase(Locale.ROOT).contains(filtro)) {
                coincidencias.add(sesion);
            }
        }
        return List.copyOf(coincidencias);
    }

    @Override
    public boolean reproducirSesion(String nombre) {
        ValidacionesGenerales.validarNombre(nombre);
        synchronized (this) {
            if (estado != EstadoControlador.CONECTADO
                    || grabando || reproduciendo) {
                notificarError(
                        "No se puede reproducir en el estado actual",
                        new IllegalStateException(
                                "Estado inválido para reproducir"));
                return false;
            }
        }
        try {
            Optional<GrabacionAbierta> grabacion = cargador.abrir(nombre);
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
            Thread hilo = new Thread(
                    () -> reproducirEnSegundoPlano(grabacion.orElseThrow()),
                    "reproduccion-" + nombre);
            hilo.setDaemon(true);
            hiloReproduccion = hilo;
            hilo.start();
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

    private void reproducirEnSegundoPlano(GrabacionAbierta grabacion) {
        try {
            reproductor.reproducir(grabacion);
        } catch (IOException | InterruptedException | RuntimeException
                | Error excepcion) {
            if (excepcion instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            notificarError(
                    "Error durante la reproducción",
                    excepcion instanceof Throwable throwable
                            ? throwable
                            : new IllegalStateException(excepcion));
        } finally {
            synchronized (this) {
                reproduciendo = false;
                hiloReproduccion = null;
                estado = EstadoControlador.CONECTADO;
            }
            cambiarEstado(EstadoControlador.CONECTADO);
        }
    }

    private void asegurarSintetizadores() throws IOException {
        if (sintetizadores.size() == 3) {
            return;
        }
        BandClient nuevoCliente = new BandClient(
                Configuraciones.getInstance());
        Map<IdSintetizador, Sintetizador> nuevos =
                new LinkedHashMap<>();
        nuevos.put(IdSintetizador.PIANO,
                new PianoSintetizador(nuevoCliente));
        nuevos.put(IdSintetizador.BAJO,
                new BajoSintetizador(nuevoCliente));
        nuevos.put(IdSintetizador.ORGANO,
                new OrganoSintetizador(nuevoCliente));
        synchronized (this) {
            bandClient = nuevoCliente;
            sintetizadores.clear();
            sintetizadores.putAll(nuevos);
        }
        for (Map.Entry<String, List<IdSintetizador>> asignacion
                : asignacionesSintetizador.entrySet()) {
            Instrumento instrumento = instrumentos.get(asignacion.getKey());
            if (instrumento != null) {
                List<Sintetizador> destinos = new ArrayList<>();
                for (IdSintetizador id : asignacion.getValue()) {
                    Sintetizador destino = sintetizadores.get(id);
                    if (destino != null) {
                        destinos.add(destino);
                    }
                }
                instrumento.reemplazarSintetizadores(destinos);
            }
        }
    }

    private void entradaDesconectada(Entrada entrada, String motivo) {
        String nombre = entrada == null ? "desconocida" : entrada.nombre();
        String mensaje = "Desconexión de entrada " + nombre
                + ": " + (motivo == null ? "dispositivo desconectado"
                        : motivo);
        notificarError(mensaje,
                new IllegalStateException(mensaje));
        desconectarRecursos();
        cambiarEstado(EstadoControlador.INICIAL);
    }

    private void desconectarPorFallo(
            String mensaje, Throwable causa) {
        LoggerPowerBand.getInstance().registrarError(
                mensaje + ": " + causa);
        notificarError(mensaje, causa);
        desconectarRecursos();
        cambiarEstado(EstadoControlador.INICIAL);
    }

    private void desconectarRecursos() {
        entradaMicrofono.detener();
        entradaControl.detener();
        entradaTableta.detener();
        if (reproductor.detener()) {
            Thread hilo = hiloReproduccion;
            if (hilo != null) {
                hilo.interrupt();
            }
        }
        for (Sintetizador sintetizador : sintetizadores.values()) {
            try {
                sintetizador.liberarTodasLasNotas();
            } catch (RuntimeException excepcion) {
                notificarError(
                        "No se pudieron liberar las notas sostenidas",
                        excepcion);
            }
        }
        if (grabando) {
            busComandos.quitarObservador(grabador);
            try {
                grabador.detener();
            } catch (IOException | RuntimeException excepcion) {
                notificarError(
                        "No se pudo detener la grabación al desconectar",
                        excepcion);
            }
        }
        if (bandClient != null) {
            try {
                bandClient.close();
            } catch (IOException excepcion) {
                notificarError("No se pudo cerrar BandClient", excepcion);
            } finally {
                bandClient = null;
                sintetizadores.clear();
            }
        }
        synchronized (this) {
            grabando = false;
            reproduciendo = false;
            estado = EstadoControlador.INICIAL;
        }
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
        LoggerPowerBand.getInstance().registrarError(
                mensaje + ": " + causa);
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
            return new Controlador();
        } catch (RuntimeException | IOException excepcion) {
            throw new ExceptionInInitializerError(excepcion);
        }
    }

    private static ComponentesProduccion crearComponentesProduccion()
            throws IOException {
        ComponentesProduccion componentes =
                new ComponentesProduccion();
        componentes.busComandos = new BusComandos();
        componentes.microfono = new Microfono(
                null, componentes.busComandos);
        componentes.control = new Control(
                null, componentes.busComandos);
        componentes.tableta = new Tableta(
                null, componentes.busComandos);
        componentes.entradaMicrofono =
                new EntradaMicrofono(componentes.microfono);
        componentes.entradaControl =
                new EntradaControl(componentes.control);
        componentes.entradaTableta =
                new EntradaTableta(componentes.tableta);
        componentes.sintetizadores = new LinkedHashMap<>();
        componentes.archivos = ArchivosGrabacion.inicializar();
        componentes.grabador = new Grabador(
                componentes.archivos.directorio());
        componentes.cargador = new CargadorGrabaciones(
                componentes.archivos);
        componentes.finalizador = new FinalizadorGrabacion(
                componentes.grabador, componentes.archivos);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(
                        id -> componentes.sintetizadores.get(id));
        componentes.reproductor = new Reproductor(
                fabrica::reconstruir,
                System::nanoTime,
                nanos -> LockSupport.parkNanos(nanos));
        return componentes;
    }

    private static final class ComponentesProduccion {
        private BusComandos busComandos;
        private Microfono microfono;
        private Control control;
        private Tableta tableta;
        private EntradaMicrofono entradaMicrofono;
        private EntradaControl entradaControl;
        private EntradaTableta entradaTableta;
        private Map<IdSintetizador, Sintetizador> sintetizadores;
        private Grabador grabador;
        private CargadorGrabaciones cargador;
        private FinalizadorGrabacion finalizador;
        private Reproductor reproductor;
        private ArchivosGrabacion archivos;
    }
}
