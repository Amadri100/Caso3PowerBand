package powerband.logging;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import powerband.util.Constantes;

public final class LoggerPowerBand {

    private static Path carpetaLogs = Paths.get(Constantes.RUTA_LOGS);

    private static final LoggerPowerBand INSTANCIA =
            new LoggerPowerBand();

    private final ReferenceQueue<Throwable> fallosLiberados = new ReferenceQueue<>();
    private final Set<ReferenciaFallo> fallosRegistrados = new HashSet<>();

    private LoggerPowerBand() {
    }

    public static LoggerPowerBand getInstance() {
        return INSTANCIA;
    }

    public static void cambiarPath(Path path) {
        Objects.requireNonNull(path, "path");
        synchronized (INSTANCIA) {
            if (!carpetaLogs.equals(path)) {
                INSTANCIA.fallosRegistrados.clear();
                carpetaLogs = path;
            }
        }
    }

    public void registrarComando(Object objeto) {
        registrar(CategoriaLog.COMANDO, objeto, null);
    }

    public void registrarInfo(Object objeto) {
        registrar(CategoriaLog.INFO, objeto, null);
    }

    public void registrarError(Object objeto) {
        registrar(CategoriaLog.ERROR, objeto, null);
    }

    public void registrarErrorArchivo(Object objeto, String ubicacion) {
        registrar(CategoriaLog.ERROR, objeto, ubicacion);
    }

    public void registrar(CategoriaLog categoria, Object objeto) {
        registrar(categoria, objeto, null);
    }

    private void registrar(CategoriaLog categoria, Object objeto, String ubicacion) {
        Objects.requireNonNull(objeto, "objeto");
        String mensaje = objeto.toString();
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El evento no puede estar vacio");
        }
        if (ubicacion != null && !ubicacion.isBlank()) {
            mensaje += " ubicacion=" + ubicacion;
        }
        registrar(Log.crear(categoria, mensaje, OrigenLog.OTRO,
                objeto instanceof Throwable ? (Throwable) objeto : null));
    }

    public synchronized void registrar(Log evento) {
        Objects.requireNonNull(evento, "evento");
        ReferenciaFallo liberado;
        while ((liberado = (ReferenciaFallo) fallosLiberados.poll()) != null) {
            fallosRegistrados.remove(liberado);
        }
        Throwable fallo = evento.throwable();
        if (fallo != null && yaRegistrado(fallo,
                Collections.newSetFromMap(new IdentityHashMap<>()))
                && !tieneSuprimidosNuevos(fallo,
                        Collections.newSetFromMap(new IdentityHashMap<>()))) {
            return;
        }
        String linea = evento.toString() + System.lineSeparator();
        try {
            Files.createDirectories(carpetaLogs);
            Path archivo = carpetaLogs.resolve("powerband.log");
            Files.writeString(archivo, linea, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (fallo != null) {
                recordar(fallo, Collections.newSetFromMap(new IdentityHashMap<>()));
            }
        } catch (IOException | SecurityException excepcion) {
            throw new IllegalStateException(
                    "No se pudo escribir el log en " + carpetaLogs, excepcion);
        }
    }

    private boolean yaRegistrado(Throwable fallo, Set<Throwable> visitados) {
        if (!visitados.add(fallo)) {
            return false;
        }
        if (contieneFallo(fallo)) {
            return true;
        }
        return fallo.getCause() != null && yaRegistrado(fallo.getCause(), visitados);
    }

    private void recordar(Throwable fallo, Set<Throwable> visitados) {
        if (!visitados.add(fallo)) {
            return;
        }
        fallosRegistrados.add(new ReferenciaFallo(fallo, fallosLiberados));
        if (fallo.getCause() != null) {
            recordar(fallo.getCause(), visitados);
        }
        for (Throwable suprimido : fallo.getSuppressed()) {
            recordar(suprimido, visitados);
        }
    }

    private boolean tieneSuprimidosNuevos(Throwable fallo, Set<Throwable> visitados) {
        if (!visitados.add(fallo)) {
            return false;
        }
        for (Throwable suprimido : fallo.getSuppressed()) {
            if (!contieneFallo(suprimido)
                    || tieneSuprimidosNuevos(suprimido, visitados)) {
                return true;
            }
        }
        return fallo.getCause() != null
                && tieneSuprimidosNuevos(fallo.getCause(), visitados);
    }

    private boolean contieneFallo(Throwable fallo) {
        return fallosRegistrados.contains(new ReferenciaFallo(fallo, null));
    }

    private static final class ReferenciaFallo extends WeakReference<Throwable> {
        private final int identidad;

        private ReferenciaFallo(Throwable fallo, ReferenceQueue<Throwable> cola) {
            super(fallo, cola);
            identidad = System.identityHashCode(fallo);
        }

        @Override
        public int hashCode() {
            return identidad;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ReferenciaFallo otra)) {
                return false;
            }
            Throwable fallo = get();
            return fallo != null && fallo == otra.get();
        }
    }
}
