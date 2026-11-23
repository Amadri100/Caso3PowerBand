package powerband.controlador;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ValidacionesGenerales;
import powerband.comandos.BusComandos;
import powerband.comandos.FabricaComandosGrabados;
import powerband.excepciones.FalloTransporteException;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.FinalizadorGrabacion;
import powerband.grabaciones.GrabacionAbierta;
import powerband.grabaciones.Grabador;
import powerband.grabaciones.LimpiezaGrabacion;
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
    private final Object bloqueoAsignaciones = new Object();
    private final Map<String, String> asignacionesDispositivo =
            new LinkedHashMap<>();
    private final Map<String, List<IdSintetizador>> asignacionesSintetizador =
            new LinkedHashMap<>();
    private final List<ObservadorEstado> observadores = new ArrayList<>();
    private final Object bloqueoCicloVida = new Object();
    private final Grabador grabador;
    private final CargadorGrabaciones cargador;
    private final FinalizadorGrabacion finalizador;
    private final Reproductor reproductor;
    private final ArchivosGrabacion archivos;
    private final Supplier<List<String>> proveedorDispositivos;

    private EstadoControlador estado = EstadoControlador.INICIAL;
    private BandClient bandClient;
    private volatile Thread hiloReproduccion;
    private boolean conectando;
    private boolean grabando;
    private boolean reproduciendo;
    private volatile boolean desconectando;

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
                componentes.archivos,
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
        // El resolutor del reproductor debe observar la misma tabla activa.
        this.sintetizadores = Objects.requireNonNull(
                sintetizadores, "sintetizadores");
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.cargador = Objects.requireNonNull(cargador, "cargador");
        this.finalizador = Objects.requireNonNull(
                finalizador, "finalizador");
        this.reproductor = Objects.requireNonNull(
                reproductor, "reproductor");
        this.archivos = Objects.requireNonNull(archivos, "archivos");
        this.proveedorDispositivos = Objects.requireNonNull(
                proveedorDispositivos, "proveedorDispositivos");

        if (microfono.busComandos() != busComandos
                || control.busComandos() != busComandos
                || tableta.busComandos() != busComandos) {
            throw new IllegalArgumentException(
                    "Todos los instrumentos deben compartir el BusComandos");
        }

        instrumentos = new LinkedHashMap<>();
        instrumentos.put("microfono", microfono);
        instrumentos.put("control", control);
        instrumentos.put("tableta", tableta);

        ObservadorDesconexion observador = (entrada, motivo, causa) ->
                entradaDesconectada(entrada, motivo, causa);
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
        return List.copyOf(Objects.requireNonNull(
                proveedorDispositivos.get(),
                "La detección devolvió una lista nula"));
    }

    @Override
    public boolean asignarDispositivo(
            String instrumentoId, String dispositivoId) {
        if (!INSTRUMENTOS.contains(instrumentoId)
                || dispositivoId == null
                || dispositivoId.isBlank()
                || !EntradaControl.esDispositivoValido(dispositivoId)) {
            return notificarAsignacionInvalida(
                    "Instrumento o dispositivo no válido");
        }

        List<String> dispositivos;
        try {
            dispositivos = listarDispositivos();
        } catch (RuntimeException excepcion) {
            notificarError(
                    "No se pudieron detectar los dispositivos",
                    excepcion);
            return false;
        }
        if (!dispositivos.contains(dispositivoId)) {
            return notificarAsignacionInvalida(
                    "El dispositivo no está disponible");
        }

        boolean dispositivoEnUso;
        RuntimeException falloAplicacion = null;
        synchronized (bloqueoAsignaciones) {
            dispositivoEnUso = asignacionesDispositivo.entrySet()
                    .stream()
                    .anyMatch(asignacion ->
                            !asignacion.getKey().equals(instrumentoId)
                                    && asignacion.getValue().equals(
                                            dispositivoId));
            if (!dispositivoEnUso) {
                if ("control".equals(instrumentoId)) {
                    String anterior =
                            asignacionesDispositivo.get(instrumentoId);
                    boolean reiniciar = entradaControl.estaActiva()
                            && !dispositivoId.equals(anterior);
                    try {
                        entradaControl.seleccionarDispositivo(dispositivoId);
                        if (reiniciar) {
                            entradaControl.detener();
                            entradaControl.iniciar();
                        }
                    } catch (RuntimeException excepcion) {
                        if (anterior != null) {
                            try {
                                entradaControl.seleccionarDispositivo(anterior);
                                if (reiniciar
                                        && !entradaControl.estaActiva()) {
                                    entradaControl.iniciar();
                                }
                            } catch (RuntimeException falloRestauracion) {
                                excepcion.addSuppressed(falloRestauracion);
                            }
                        }
                        falloAplicacion = excepcion;
                    }
                }
                if (falloAplicacion == null) {
                    asignacionesDispositivo.put(
                            instrumentoId, dispositivoId);
                }
            }
        }
        if (dispositivoEnUso) {
            return notificarAsignacionInvalida(
                    "El dispositivo ya está asignado a otro instrumento");
        }
        if (falloAplicacion != null) {
            notificarError(
                    "No se pudo aplicar la asignación del dispositivo",
                    falloAplicacion);
            return false;
        }
        return true;
    }

    @Override
    public boolean asignarSintetizador(
            String instrumentoId, IdSintetizador sintetizadorId) {
        if (sintetizadorId == null) {
            return notificarAsignacionInvalida(
                    "El sintetizador no es válido");
        }
        return reemplazarSintetizadores(
                instrumentoId, List.of(sintetizadorId));
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
        synchronized (bloqueoAsignaciones) {
            return Collections.unmodifiableMap(
                    new LinkedHashMap<>(asignacionesDispositivo));
        }
    }

    @Override
    public Map<String, List<IdSintetizador>>
            sintetizadoresPorInstrumento() {
        synchronized (bloqueoAsignaciones) {
            Map<String, List<IdSintetizador>> copia =
                    new LinkedHashMap<>();
            for (Map.Entry<String, List<IdSintetizador>> asignacion
                    : asignacionesSintetizador.entrySet()) {
                copia.put(asignacion.getKey(),
                        List.copyOf(asignacion.getValue()));
            }
            return Collections.unmodifiableMap(copia);
        }
    }

    @Override
    public boolean reemplazarSintetizadores(
            String instrumentoId,
            List<IdSintetizador> sintetizadorIds) {
        Objects.requireNonNull(sintetizadorIds, "sintetizadorIds");
        Instrumento instrumento = instrumentos.get(instrumentoId);
        if (instrumento == null) {
            return notificarAsignacionInvalida(
                    "Instrumento no válido");
        }

        Set<IdSintetizador> idsUnicos = new LinkedHashSet<>(sintetizadorIds);
        if (idsUnicos.stream().anyMatch(
                sintetizadorId -> sintetizadorId == null
                        || !listarSintetizadores().contains(sintetizadorId))) {
            return notificarAsignacionInvalida(
                    "Uno o más sintetizadores no son válidos");
        }

        String sintetizadorNoDisponible = null;
        synchronized (bloqueoAsignaciones) {
            List<Sintetizador> destinos = new ArrayList<>();
            for (IdSintetizador id : idsUnicos) {
                Sintetizador sintetizador = sintetizadores.get(id);
                if (!sintetizadores.isEmpty() && sintetizador == null) {
                    sintetizadorNoDisponible =
                            "No está disponible el sintetizador " + id;
                    break;
                }
                if (sintetizador != null) {
                    destinos.add(sintetizador);
                }
            }
            if (sintetizadorNoDisponible == null) {
                instrumento.reemplazarSintetizadores(destinos);
                asignacionesSintetizador.put(
                        instrumentoId, List.copyOf(idsUnicos));
            }
        }
        if (sintetizadorNoDisponible != null) {
            return notificarAsignacionInvalida(sintetizadorNoDisponible);
        }
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
        busComandos.quitarObservador(grabador);
        try {
            finalizador.finalizar(nombre);
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
                hiloReproduccion = null;
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
        boolean falloGlobal = false;
        try {
            reproductor.reproducir(grabacion);
        } catch (IOException | InterruptedException | RuntimeException
                | Error excepcion) {
            if (excepcion instanceof InterruptedException) {
                Thread.currentThread().interrupt();
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
                hiloReproduccion = null;
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
        synchronized (bloqueoAsignaciones) {
            for (Map.Entry<String, List<IdSintetizador>> asignacion
                    : asignacionesSintetizador.entrySet()) {
                Instrumento instrumento =
                        instrumentos.get(asignacion.getKey());
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
            synchronized (bloqueoAsignaciones) {
                asignacionesDispositivo.remove(nombre);
                if ("control".equals(nombre)) {
                    entradaControl.limpiarDispositivoSeleccionado();
                }
            }
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
        boolean eraDesconectando;
        synchronized (this) {
            eraDesconectando = desconectando;
            desconectando = true;
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
            if (!eraDesconectando) {
                desconectando = false;
            }
        }
    }

    private Throwable desconectarRecursos(Throwable falloPrincipal) {
        Throwable fallo = falloPrincipal;
        fallo = intentarDetenerEntrada(
                fallo, entradaMicrofono, "micrófono");
        fallo = intentarDetenerEntrada(
                fallo, entradaControl, "control");
        fallo = intentarDetenerEntrada(
                fallo, entradaTableta, "tableta");

        if (grabando || grabador.estaGrabando()) {
            busComandos.quitarObservador(grabador);
            Path archivoTemporal = null;
            try {
                archivoTemporal = grabador.archivoSesionTemporal();
            } catch (RuntimeException excepcion) {
                fallo = agregarFallo(
                        fallo,
                        "No se pudo localizar el temporal de grabación",
                        excepcion);
            }
            try {
                if (grabador.estaGrabando()) {
                    grabador.detener();
                }
            } catch (IOException | RuntimeException excepcion) {
                fallo = agregarFallo(
                        fallo,
                        "No se pudo detener la grabación al desconectar",
                        excepcion);
            }
            if (archivoTemporal != null) {
                try {
                    new LimpiezaGrabacion(
                            archivoTemporal, archivos)
                            .limpiarExitosamente();
                } catch (IOException | RuntimeException excepcion) {
                    fallo = agregarFallo(
                            fallo,
                            "No se pudo limpiar el temporal de grabación",
                            excepcion);
                }
            }
        }

        boolean interrumpido = false;
        try {
            reproductor.detener();
        } catch (RuntimeException excepcion) {
            fallo = agregarFallo(
                    fallo, "No se pudo detener la reproducción", excepcion);
        }
        Thread hilo = hiloReproduccion;
        if (hilo != null && hilo != Thread.currentThread()) {
            if (hilo.isAlive()) {
                hilo.interrupt();
            }
            interrumpido = esperarTerminacion(hilo);
        }
        if (interrumpido) {
            Thread.currentThread().interrupt();
            fallo = agregarFallo(
                    fallo,
                    "Se interrumpió la espera de finalización de la reproducción",
                    new InterruptedException(
                            "La espera de reproducción fue interrumpida"));
        }

        for (Sintetizador sintetizador
                : List.copyOf(sintetizadores.values())) {
            try {
                sintetizador.liberarTodasLasNotas();
            } catch (RuntimeException excepcion) {
                fallo = agregarFallo(
                        fallo,
                        "No se pudieron liberar las notas sostenidas",
                        excepcion);
            }
        }

        if (bandClient != null) {
            try {
                bandClient.close();
            } catch (IOException | RuntimeException excepcion) {
                fallo = agregarFallo(
                        fallo, "No se pudo cerrar BandClient", excepcion);
            } finally {
                bandClient = null;
                for (Instrumento instrumento : instrumentos.values()) {
                    try {
                        instrumento.reemplazarSintetizadores(List.of());
                    } catch (RuntimeException excepcion) {
                        fallo = agregarFallo(
                                fallo,
                                "No se pudo liberar un sintetizador asignado",
                                excepcion);
                    }
                }
                sintetizadores.clear();
            }
        }
        synchronized (this) {
            grabando = false;
            reproduciendo = false;
            estado = EstadoControlador.INICIAL;
        }
        return fallo;
    }

    private Throwable intentarDetenerEntrada(
            Throwable fallo,
            Entrada entrada,
            String nombre) {
        try {
            entrada.detener();
        } catch (RuntimeException excepcion) {
            return agregarFallo(
                    fallo,
                    "No se pudo detener la entrada " + nombre,
                    excepcion);
        }
        return fallo;
    }

    private boolean hayRecursosActivos() {
        synchronized (this) {
            if (estado != EstadoControlador.INICIAL
                    || bandClient != null || conectando || grabando
                    || reproduciendo || hiloReproduccion != null) {
                return true;
            }
        }
        return grabador.estaGrabando()
                || entradaMicrofono.estaActiva()
                || entradaControl.estaActiva()
                || entradaTableta.estaActiva();
    }

    private static Throwable agregarFallo(
            Throwable principal, String mensaje, Throwable causa) {
        RuntimeException contextual = new IllegalStateException(
                mensaje, causa);
        if (principal == null) {
            return contextual;
        }
        if (principal != contextual && principal != causa) {
            principal.addSuppressed(contextual);
        }
        return principal;
    }

    private static RuntimeException convertirEnRuntime(
            String mensaje, Throwable causa) {
        return causa instanceof RuntimeException runtime
                ? runtime
                : new IllegalStateException(mensaje, causa);
    }

    private static boolean esperarTerminacion(Thread hilo) {
        boolean interrumpido = false;
        while (hilo.isAlive()) {
            try {
                hilo.join();
            } catch (InterruptedException excepcion) {
                interrumpido = true;
            }
        }
        return interrumpido;
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

    private boolean notificarAsignacionInvalida(String mensaje) {
        IllegalArgumentException excepcion =
                new IllegalArgumentException(mensaje);
        notificarError(mensaje, excepcion);
        return false;
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
        componentes.sintetizadores = new ConcurrentHashMap<>();
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
