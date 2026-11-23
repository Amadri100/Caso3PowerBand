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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import powerband.input.Entrada;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.input.ObservadorDesconexion;
import powerband.instrumentos.Control;
import powerband.instrumentos.Instrumento;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.sintetizadores.BandClient;
import powerband.sintetizadores.BajoSintetizador;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.OrganoSintetizador;
import powerband.sintetizadores.PianoSintetizador;
import powerband.sintetizadores.Sintetizador;
import powerband.util.Configuraciones;

final class ControladorDispositivos {

    private static final List<String> INSTRUMENTOS = List.of(
            "microfono", "control", "tableta");

    private final EntradaMicrofono entradaMicrofono;
    private final EntradaControl entradaControl;
    private final EntradaTableta entradaTableta;
    private final Map<String, Instrumento> instrumentos = new LinkedHashMap<>();
    private final Map<IdSintetizador, Sintetizador> sintetizadores;
    private final Object bloqueoAsignaciones = new Object();
    private final Map<String, String> asignacionesDispositivo = new LinkedHashMap<>();
    private final Map<String, List<IdSintetizador>> asignacionesSintetizador =
            new LinkedHashMap<>();
    private final Supplier<List<String>> proveedorDispositivos;
    private final BiConsumer<String, Throwable> notificarError;
    private volatile BandClient bandClient;

    ControladorDispositivos(
            Microfono microfono, Control control, Tableta tableta,
            EntradaMicrofono entradaMicrofono, EntradaControl entradaControl,
            EntradaTableta entradaTableta,
            Map<IdSintetizador, Sintetizador> sintetizadores,
            Supplier<List<String>> proveedorDispositivos,
            BiConsumer<String, Throwable> notificarError,
            ObservadorDesconexion observadorDesconexion) {
        this.entradaMicrofono = Objects.requireNonNull(entradaMicrofono, "entradaMicrofono");
        this.entradaControl = Objects.requireNonNull(entradaControl, "entradaControl");
        this.entradaTableta = Objects.requireNonNull(entradaTableta, "entradaTableta");
        this.sintetizadores = Objects.requireNonNull(sintetizadores, "sintetizadores");
        this.proveedorDispositivos = Objects.requireNonNull(proveedorDispositivos, "proveedorDispositivos");
        this.notificarError = Objects.requireNonNull(notificarError, "notificarError");
        instrumentos.put("microfono", Objects.requireNonNull(microfono, "microfono"));
        instrumentos.put("control", Objects.requireNonNull(control, "control"));
        instrumentos.put("tableta", Objects.requireNonNull(tableta, "tableta"));
        entradaMicrofono.registrarObservadorDesconexion(observadorDesconexion);
        entradaControl.registrarObservadorDesconexion(observadorDesconexion);
        entradaTableta.registrarObservadorDesconexion(observadorDesconexion);
    }

    List<String> listarDispositivos() {
        return List.copyOf(Objects.requireNonNull(
                proveedorDispositivos.get(), "La detección devolvió una lista nula"));
    }

    boolean asignarDispositivo(String instrumentoId, String dispositivoId) {
        if (!INSTRUMENTOS.contains(instrumentoId) || dispositivoId == null
                || dispositivoId.isBlank()
                || !EntradaControl.esDispositivoValido(dispositivoId)) {
            return asignacionInvalida("Instrumento o dispositivo no válido");
        }
        List<String> dispositivos;
        try {
            dispositivos = listarDispositivos();
        } catch (RuntimeException excepcion) {
            notificarError.accept("No se pudieron detectar los dispositivos", excepcion);
            return false;
        }
        if (!dispositivos.contains(dispositivoId)) {
            return asignacionInvalida("El dispositivo no está disponible");
        }

        boolean dispositivoEnUso;
        RuntimeException falloAplicacion = null;
        synchronized (bloqueoAsignaciones) {
            dispositivoEnUso = asignacionesDispositivo.entrySet().stream()
                    .anyMatch(asignacion -> !asignacion.getKey().equals(instrumentoId)
                            && asignacion.getValue().equals(dispositivoId));
            if (!dispositivoEnUso) {
                if ("control".equals(instrumentoId)) {
                    String anterior = asignacionesDispositivo.get(instrumentoId);
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
                                if (reiniciar && !entradaControl.estaActiva()) {
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
                    asignacionesDispositivo.put(instrumentoId, dispositivoId);
                }
            }
        }
        if (dispositivoEnUso) {
            return asignacionInvalida("El dispositivo ya está asignado a otro instrumento");
        }
        if (falloAplicacion != null) {
            notificarError.accept("No se pudo aplicar la asignación del dispositivo", falloAplicacion);
            return false;
        }
        return true;
    }

    boolean asignarSintetizador(String instrumentoId, IdSintetizador sintetizadorId) {
        if (sintetizadorId == null) {
            return asignacionInvalida("El sintetizador no es válido");
        }
        return reemplazarSintetizadores(instrumentoId, List.of(sintetizadorId));
    }

    List<String> listarInstrumentos() {
        return INSTRUMENTOS;
    }

    List<IdSintetizador> listarSintetizadores() {
        return List.of(IdSintetizador.PIANO, IdSintetizador.BAJO, IdSintetizador.ORGANO);
    }

    Map<String, String> dispositivoPorInstrumento() {
        synchronized (bloqueoAsignaciones) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(asignacionesDispositivo));
        }
    }

    Map<String, List<IdSintetizador>> sintetizadoresPorInstrumento() {
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
    }

    private void asegurarSintetizadores() throws IOException {
        if (sintetizadores.size() == 3) {
            return;
        }
        BandClient nuevoCliente = new BandClient(Configuraciones.getInstance());
        Map<IdSintetizador, Sintetizador> nuevos = new LinkedHashMap<>();
        nuevos.put(IdSintetizador.PIANO, new PianoSintetizador(nuevoCliente));
        nuevos.put(IdSintetizador.BAJO, new BajoSintetizador(nuevoCliente));
        nuevos.put(IdSintetizador.ORGANO, new OrganoSintetizador(nuevoCliente));
        bandClient = nuevoCliente;
        sintetizadores.clear();
        sintetizadores.putAll(nuevos);
        synchronized (bloqueoAsignaciones) {
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
            if ("control".equals(nombre)) {
                entradaControl.limpiarDispositivoSeleccionado();
            }
        }
    }

    boolean hayRecursosActivos() {
        return bandClient != null || entradaMicrofono.estaActiva()
                || entradaControl.estaActiva() || entradaTableta.estaActiva();
    }

    Throwable detenerEntradas(Throwable fallo) {
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

    Throwable liberar(Throwable fallo) {
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
