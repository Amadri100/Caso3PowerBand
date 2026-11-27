package powerband.controlador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import powerband.util.DispositivoEntrada;
import powerband.input.DisponibilidadDispositivo;
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
import powerband.logging.OrigenLog;
import powerband.sintetizadores.BandClient;
import powerband.sintetizadores.BajoSintetizador;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.OrganoSintetizador;
import powerband.sintetizadores.PianoSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Configuraciones;
import powerband.util.Constantes;

final class ControladorDispositivos {

    private static final List<String> INSTRUMENTOS = List.of(
            "microfono", "control", "tableta");

    private final EntradaMicrofono entradaMicrofono;
    private final EntradaControl entradaControl;
    private final EntradaTableta entradaTableta;
    private final Map<String, Instrumento> instrumentos = new LinkedHashMap<>();
    private final Map<IdSintetizador, Sintetizador> sintetizadores;
    private final Object bloqueoAsignaciones = new Object();
    private final Object bloqueoCambioDispositivo = new Object();
    private final Map<String, String> asignacionesDispositivo = new LinkedHashMap<>();
    private final Map<String, Long> versionesDesconexion = new LinkedHashMap<>();
    private final Map<String, List<IdSintetizador>> asignacionesSintetizador =
            new LinkedHashMap<>();
    private final Function<String, List<DispositivoEntrada>> proveedorDispositivos;
    private final boolean deteccionReal;
    private final BiFunction<String, String, DisponibilidadDispositivo> comprobadorDisponibilidad;
    private final Map<String, String> fallosComprobacion = new ConcurrentHashMap<>();
    private final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "monitor-dispositivos");
        hilo.setDaemon(true);
        return hilo;
    });
    private boolean monitorIniciado;
    private final BiConsumer<String, Throwable> notificarError;
    private volatile BandClient bandClient;

    protected ControladorDispositivos(
            Microfono microfono, Control control, Tableta tableta,
            EntradaMicrofono entradaMicrofono, EntradaControl entradaControl,
            EntradaTableta entradaTableta,
            Map<IdSintetizador, Sintetizador> sintetizadores,
            Function<String, List<DispositivoEntrada>> proveedorDispositivos,
            BiConsumer<String, Throwable> notificarError,
            ObservadorDesconexion observadorDesconexion) {
        this(microfono, control, tableta, entradaMicrofono, entradaControl, entradaTableta,
                sintetizadores, proveedorDispositivos, null, notificarError, observadorDesconexion);
    }

    protected ControladorDispositivos(
            Microfono microfono, Control control, Tableta tableta,
            EntradaMicrofono entradaMicrofono, EntradaControl entradaControl,
            EntradaTableta entradaTableta,
            Map<IdSintetizador, Sintetizador> sintetizadores,
            Function<String, List<DispositivoEntrada>> proveedorDispositivos,
            BiFunction<String, String, DisponibilidadDispositivo> comprobadorDisponibilidad,
            BiConsumer<String, Throwable> notificarError,
            ObservadorDesconexion observadorDesconexion) {
        this.entradaMicrofono = Objects.requireNonNull(entradaMicrofono, "entradaMicrofono");
        this.entradaControl = Objects.requireNonNull(entradaControl, "entradaControl");
        this.entradaTableta = Objects.requireNonNull(entradaTableta, "entradaTableta");
        this.sintetizadores = Objects.requireNonNull(sintetizadores, "sintetizadores");
        this.deteccionReal = proveedorDispositivos == null;
        this.proveedorDispositivos = proveedorDispositivos == null
                ? this::detectarDispositivos : proveedorDispositivos;
        this.comprobadorDisponibilidad = comprobadorDisponibilidad == null
                ? this::comprobarDisponibilidad : comprobadorDisponibilidad;
        this.notificarError = Objects.requireNonNull(notificarError, "notificarError");
        instrumentos.put("microfono", Objects.requireNonNull(microfono, "microfono"));
        instrumentos.put("control", Objects.requireNonNull(control, "control"));
        instrumentos.put("tableta", Objects.requireNonNull(tableta, "tableta"));
        entradaMicrofono.registrarObservadorDesconexion(observadorDesconexion);
        entradaControl.registrarObservadorDesconexion(observadorDesconexion);
        entradaTableta.registrarObservadorDesconexion(observadorDesconexion);
        entradaTableta.registrarObservadorRetiro(this::programarRetiroTableta);
    }

    protected List<String> listarDispositivos() {
        return listarDispositivos("control").stream()
                .map(DispositivoEntrada::id).toList();
    }

    protected List<DispositivoEntrada> listarDispositivos(String instrumentoId) {
        if (instrumentoId == null || !INSTRUMENTOS.contains(instrumentoId)) {
            IllegalArgumentException fallo = new IllegalArgumentException("Instrumento no válido");
            notificarError.accept(fallo.getMessage(), fallo);
            throw fallo;
        }
        try {
            return List.copyOf(Objects.requireNonNull(
                    proveedorDispositivos.apply(instrumentoId),
                    "La detección devolvió una lista nula"));
        } catch (RuntimeException fallo) {
            notificarError.accept("No se pudieron detectar los dispositivos", fallo);
            throw fallo;
        } catch (LinkageError fallo) {
            IllegalStateException error = new IllegalStateException(
                    "No se pudo cargar la biblioteca de dispositivos", fallo);
            notificarError.accept(error.getMessage(), error);
            throw error;
        }
    }

    private List<DispositivoEntrada> detectarDispositivos(String instrumentoId) {
        return switch (instrumentoId) {
            case "control" -> EntradaControl.dispositivosCompatibles();
            case "microfono" -> EntradaMicrofono.dispositivosCompatibles();
            case "tableta" -> entradaTableta.dispositivosCompatibles();
            default -> throw new IllegalArgumentException("Instrumento no válido");
        };
    }

    protected java.awt.Component superficieTableta() {
        return entradaTableta.superficie();
    }

    void establecerModoDescubrimientoTableta(boolean activo) {
        entradaTableta.establecerModoDescubrimiento(activo);
    }

    boolean asignarDispositivo(String instrumentoId, String dispositivoId) {
        if (instrumentoId == null || !INSTRUMENTOS.contains(instrumentoId) || dispositivoId == null
                || dispositivoId.isBlank()) {
            return asignacionInvalida("Instrumento o dispositivo no válido");
        }
        List<DispositivoEntrada> disponibles;
        try {
            disponibles = listarDispositivos(instrumentoId);
        } catch (RuntimeException excepcion) {
            LoggerPowerBand.getInstance().registrarExcepcionNoCapturada(
                    excepcion, OrigenLog.CONTROLADOR);
            return false;
        }
        if (disponibles.stream().noneMatch(dispositivo -> dispositivo.id().equals(dispositivoId))) {
            return asignacionInvalida("El dispositivo no está disponible");
        }
        try {
            if (deteccionReal && "control".equals(instrumentoId)
                    && !EntradaControl.dispositivoDisponible(dispositivoId)) {
                return asignacionInvalida("El control no responde o ya no está disponible");
            }
        } catch (RuntimeException excepcion) {
            notificarError.accept("No se pudo comprobar el control", excepcion);
            return false;
        }

        synchronized (bloqueoCambioDispositivo) {
            String anterior;
            long version;
            synchronized (bloqueoAsignaciones) {
                boolean dispositivoEnUso = asignacionesDispositivo.entrySet().stream()
                        .anyMatch(asignacion -> !asignacion.getKey().equals(instrumentoId)
                                && asignacion.getValue().equals(dispositivoId));
                if (dispositivoEnUso) {
                    return asignacionInvalida("El dispositivo ya está asignado a otro instrumento");
                }
                anterior = asignacionesDispositivo.get(instrumentoId);
                version = versionesDesconexion.getOrDefault(instrumentoId, 0L);
            }
            Entrada entrada = entrada(instrumentoId);
            boolean reiniciar = entrada.estaActiva() && !dispositivoId.equals(anterior)
                    && !"tableta".equals(instrumentoId);
            try {
                if (reiniciar) {
                    entrada.detener();
                }
                synchronized (bloqueoAsignaciones) {
                    if (versionesDesconexion.getOrDefault(instrumentoId, 0L) != version) {
                        return asignacionInvalida("La entrada se desconectó durante la asignación");
                    }
                    seleccionar(instrumentoId, dispositivoId);
                }
                if (reiniciar) {
                    entrada.iniciar();
                }
                synchronized (bloqueoAsignaciones) {
                    if (versionesDesconexion.getOrDefault(instrumentoId, 0L) != version) {
                        return asignacionInvalida("La entrada se desconectó durante la asignación");
                    }
                    asignacionesDispositivo.put(instrumentoId, dispositivoId);
                    versionesDesconexion.merge(instrumentoId, 1L, Long::sum);
                }
                iniciarMonitor();
                return true;
            } catch (RuntimeException excepcion) {
                synchronized (bloqueoAsignaciones) {
                    if (versionesDesconexion.getOrDefault(instrumentoId, 0L) == version) {
                        try {
                            if (anterior == null) {
                                limpiarSeleccion(instrumentoId);
                            } else {
                                seleccionar(instrumentoId, anterior);
                            }
                            if (reiniciar && !entrada.estaActiva()) {
                                entrada.iniciar();
                            }
                        } catch (RuntimeException falloRestauracion) {
                            excepcion.addSuppressed(falloRestauracion);
                        }
                    }
                }
                notificarError.accept("No se pudo aplicar la asignación del dispositivo", excepcion);
                return false;
            }
        }
    }

    private Entrada entrada(String instrumentoId) {
        return switch (instrumentoId) {
            case "control" -> entradaControl;
            case "microfono" -> entradaMicrofono;
            case "tableta" -> entradaTableta;
            default -> throw new IllegalArgumentException("Instrumento no válido");
        };
    }

    private synchronized void iniciarMonitor() {
        if (deteccionReal && !monitorIniciado) {
            monitor.scheduleWithFixedDelay(this::ejecutarMonitor,
                    Constantes.INTERVALO_COMPROBACION_DISPOSITIVOS_SEGUNDOS,
                    Constantes.INTERVALO_COMPROBACION_DISPOSITIVOS_SEGUNDOS, TimeUnit.SECONDS);
            monitorIniciado = true;
        }
    }

    private void ejecutarMonitor() {
        try {
            verificarDesconexiones();
        } catch (RuntimeException | LinkageError fallo) {
            informarFalloComprobacion("monitor", fallo);
        }
    }

    void verificarDesconexiones() {
        Map<String, AsignacionVigilada> copia = new LinkedHashMap<>();
        synchronized (bloqueoAsignaciones) {
            asignacionesDispositivo.forEach((instrumento, id) ->
                    copia.put(instrumento, new AsignacionVigilada(
                            id, versionesDesconexion.getOrDefault(instrumento, 0L))));
        }
        copia.forEach((instrumento, asignacion) -> {
            if ("tableta".equals(instrumento)
                    || ("control".equals(instrumento) && entradaControl.estaActiva())) {
                return;
            }
            try {
                DisponibilidadDispositivo disponibilidad = Objects.requireNonNull(
                        comprobadorDisponibilidad.apply(instrumento, asignacion.id()),
                        "La comprobación de disponibilidad devolvió null");
                if (disponibilidad == DisponibilidadDispositivo.AMBIGUO) {
                    informarFalloComprobacion(instrumento,
                            new IllegalStateException("La identidad del dispositivo es ambigua"));
                } else {
                    fallosComprobacion.remove(instrumento);
                    if (disponibilidad == DisponibilidadDispositivo.NO_DISPONIBLE) {
                        retirarSiVigente(instrumento, asignacion);
                    }
                }
            } catch (RuntimeException | LinkageError fallo) {
                informarFalloComprobacion(instrumento, fallo);
            }
        });
    }

    private DisponibilidadDispositivo comprobarDisponibilidad(String instrumento, String id) {
        return switch (instrumento) {
            case "control" -> EntradaControl.comprobarDisponibilidad(id);
            case "microfono" -> EntradaMicrofono.comprobarDisponibilidad(id);
            default -> throw new IllegalArgumentException("La entrada no admite comprobación periódica");
        };
    }

    private void informarFalloComprobacion(String instrumento, Throwable fallo) {
        String detalle = fallo.toString();
        if (!detalle.equals(fallosComprobacion.put(instrumento, detalle))) {
            notificarError.accept("No se pudo comprobar la disponibilidad de " + instrumento, fallo);
        }
    }

    private void programarRetiroTableta(String id) {
        AsignacionVigilada asignacion;
        synchronized (bloqueoAsignaciones) {
            if (!id.equals(asignacionesDispositivo.get("tableta"))) {
                return;
            }
            asignacion = new AsignacionVigilada(id,
                    versionesDesconexion.getOrDefault("tableta", 0L));
        }
        monitor.execute(() -> {
            try {
                retirarSiVigente("tableta", asignacion);
            } catch (RuntimeException fallo) {
                notificarError.accept("No se pudo procesar el retiro de la tableta", fallo);
            }
        });
    }

    private void retirarSiVigente(String instrumento, AsignacionVigilada asignacion) {
        synchronized (bloqueoCambioDispositivo) {
            synchronized (bloqueoAsignaciones) {
                if (!asignacion.id().equals(asignacionesDispositivo.get(instrumento))
                        || asignacion.version() != versionesDesconexion.getOrDefault(instrumento, 0L)) {
                    return;
                }
            }
            entrada(instrumento).detener();
            synchronized (bloqueoAsignaciones) {
                if (!asignacion.id().equals(asignacionesDispositivo.get(instrumento))
                        || asignacion.version() != versionesDesconexion.getOrDefault(instrumento, 0L)) {
                    return;
                }
            }
            entrada(instrumento).desconectar("el dispositivo ya no está disponible");
        }
    }

    private record AsignacionVigilada(String id, long version) {
    }

    private void seleccionar(String instrumentoId, String dispositivoId) {
        switch (instrumentoId) {
            case "control" -> entradaControl.seleccionarDispositivo(dispositivoId);
            case "microfono" -> entradaMicrofono.seleccionarDispositivo(dispositivoId);
            case "tableta" -> entradaTableta.seleccionarDispositivo(dispositivoId);
            default -> throw new IllegalArgumentException("Instrumento no válido");
        }
    }

    private void limpiarSeleccion(String instrumentoId) {
        switch (instrumentoId) {
            case "control" -> entradaControl.limpiarDispositivoSeleccionado();
            case "microfono" -> entradaMicrofono.limpiarDispositivoSeleccionado();
            case "tableta" -> entradaTableta.limpiarDispositivoSeleccionado();
            default -> throw new IllegalArgumentException("Instrumento no válido");
        }
    }

    boolean asignarSintetizador(String instrumentoId, IdSintetizador sintetizadorId) {
        if (sintetizadorId == null) {
            return asignacionInvalida("El sintetizador no es válido");
        }
        return reemplazarSintetizadores(instrumentoId, List.of(sintetizadorId));
    }

    protected List<String> listarInstrumentos() {
        return INSTRUMENTOS;
    }

    protected List<IdSintetizador> listarSintetizadores() {
        return List.of(IdSintetizador.PIANO, IdSintetizador.BAJO, IdSintetizador.ORGANO);
    }

    protected Map<String, String> dispositivoPorInstrumento() {
        synchronized (bloqueoAsignaciones) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(asignacionesDispositivo));
        }
    }

    protected Map<String, List<IdSintetizador>> sintetizadoresPorInstrumento() {
        synchronized (bloqueoAsignaciones) {
            Map<String, List<IdSintetizador>> copia = new LinkedHashMap<>();
            for (Map.Entry<String, List<IdSintetizador>> asignacion
                    : asignacionesSintetizador.entrySet()) {
                copia.put(asignacion.getKey(), List.copyOf(asignacion.getValue()));
            }
            return Collections.unmodifiableMap(copia);
        }
    }

    boolean reemplazarSintetizadores(
            String instrumentoId, List<IdSintetizador> sintetizadorIds) {
        Objects.requireNonNull(sintetizadorIds, "sintetizadorIds");
        Instrumento instrumento = instrumentos.get(instrumentoId);
        if (instrumento == null) {
            return asignacionInvalida("Instrumento no válido");
        }
        Set<IdSintetizador> idsUnicos = new LinkedHashSet<>(sintetizadorIds);
        if (idsUnicos.stream().anyMatch(id -> id == null
                || !listarSintetizadores().contains(id))) {
            return asignacionInvalida("Uno o más sintetizadores no son válidos");
        }
        String sintetizadorNoDisponible = null;
        synchronized (bloqueoAsignaciones) {
            List<Sintetizador> destinos = new ArrayList<>();
            for (IdSintetizador id : idsUnicos) {
                Sintetizador sintetizador = sintetizadores.get(id);
                if (!sintetizadores.isEmpty() && sintetizador == null) {
                    sintetizadorNoDisponible = "No está disponible el sintetizador " + id;
                    break;
                }
                if (sintetizador != null) {
                    destinos.add(sintetizador);
                }
            }
            if (sintetizadorNoDisponible == null) {
                instrumento.reemplazarSintetizadores(destinos);
                asignacionesSintetizador.put(instrumentoId, List.copyOf(idsUnicos));
            }
        }
        if (sintetizadorNoDisponible != null) {
            return asignacionInvalida(sintetizadorNoDisponible);
        }
        return true;
    }

    void iniciar() throws IOException {
        asegurarSintetizadores();
        entradaMicrofono.iniciar();
        entradaControl.iniciar();
        entradaTableta.iniciar();
        if (bandClient != null) {
            bandClient.playConnectionTone();
        }
    }

    private void asegurarSintetizadores() throws IOException {
        if (sintetizadores.size() != 3) {
            BandClient nuevoCliente = new BandClient(Configuraciones.getInstance());
            Map<IdSintetizador, Sintetizador> nuevos = new LinkedHashMap<>();
            nuevos.put(IdSintetizador.PIANO, new PianoSintetizador(nuevoCliente));
            nuevos.put(IdSintetizador.BAJO, new BajoSintetizador(nuevoCliente));
            nuevos.put(IdSintetizador.ORGANO, new OrganoSintetizador(nuevoCliente));
            bandClient = nuevoCliente;
            sintetizadores.clear();
            sintetizadores.putAll(nuevos);
        }
        synchronized (bloqueoAsignaciones) {
            Sintetizador piano = sintetizadores.get(IdSintetizador.PIANO);
            for (String instrumentoId : INSTRUMENTOS) {
                if (!asignacionesSintetizador.containsKey(instrumentoId)) {
                    instrumentos.get(instrumentoId).reemplazarSintetizadores(
                            List.of(piano));
                    asignacionesSintetizador.put(
                            instrumentoId, List.of(IdSintetizador.PIANO));
                }
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
    }

    void retirarDispositivo(String nombre) {
        synchronized (bloqueoAsignaciones) {
            asignacionesDispositivo.remove(nombre);
            limpiarSeleccion(nombre);
            versionesDesconexion.merge(nombre, 1L, Long::sum);
        }
    }

    boolean hayRecursosActivos() {
        return bandClient != null || entradaMicrofono.estaActiva()
                || entradaControl.estaActiva() || entradaTableta.estaActiva();
    }

    protected Throwable detenerEntradas(Throwable fallo) {
        fallo = detenerEntrada(fallo, entradaMicrofono, "micrófono");
        fallo = detenerEntrada(fallo, entradaControl, "control");
        return detenerEntrada(fallo, entradaTableta, "tableta");
    }

    private Throwable detenerEntrada(Throwable fallo, Entrada entrada, String nombre) {
        try {
            entrada.detener();
        } catch (RuntimeException excepcion) {
            return FallosControlador.agregar(
                    fallo, "No se pudo detener la entrada " + nombre, excepcion);
        }
        return fallo;
    }

    protected Throwable liberar(Throwable fallo) {
        for (Sintetizador sintetizador : List.copyOf(sintetizadores.values())) {
            try {
                sintetizador.liberarTodasLasNotas();
            } catch (RuntimeException excepcion) {
                fallo = FallosControlador.agregar(
                        fallo, "No se pudieron liberar las notas sostenidas", excepcion);
            }
        }
        if (bandClient != null) {
            try {
                bandClient.close();
            } catch (IOException | RuntimeException excepcion) {
                fallo = FallosControlador.agregar(fallo, "No se pudo cerrar BandClient", excepcion);
            } finally {
                bandClient = null;
                for (Instrumento instrumento : instrumentos.values()) {
                    try {
                        instrumento.reemplazarSintetizadores(List.of());
                    } catch (RuntimeException excepcion) {
                        fallo = FallosControlador.agregar(
                                fallo, "No se pudo liberar un sintetizador asignado", excepcion);
                    }
                }
                sintetizadores.clear();
            }
        }
        return fallo;
    }

    private boolean asignacionInvalida(String mensaje) {
        notificarError.accept(mensaje, new IllegalArgumentException(mensaje));
        return false;
    }
}
