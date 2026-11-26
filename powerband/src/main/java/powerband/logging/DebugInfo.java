package powerband.logging;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class DebugInfo {

    private final OrigenLog origen;
    private final Throwable throwable;

    public DebugInfo(OrigenLog origen, Throwable throwable) {
        this.origen = Objects.requireNonNull(origen, "origen");
        this.throwable = throwable;
    }

    Throwable throwable() {
        return throwable;
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder("origen=").append(origen);
        if (throwable != null) {
            texto.append(" throwable=");
            Set<Throwable> visitados = Collections.newSetFromMap(new IdentityHashMap<>());
            agregarThrowable(texto, throwable, visitados);
            StackTraceElement[] traza = throwable.getStackTrace();
            if (traza.length == 0) {
                texto.append(" ubicacionError=desconocida");
            } else {
                StackTraceElement lugar = traza[0];
                texto.append(" ubicacionError=").append(lugar.getClassName())
                        .append('.').append(lugar.getMethodName()).append('(')
                        .append(lugar.getFileName() == null ? "archivo desconocido" : lugar.getFileName())
                        .append(':')
                        .append(lugar.getLineNumber() < 0 ? "línea desconocida" : lugar.getLineNumber())
                        .append(')');
            }
        }
        return texto.toString();
    }

    private static void agregarThrowable(
            StringBuilder texto, Throwable fallo, Set<Throwable> visitados) {
        if (!visitados.add(fallo)) {
            texto.append("[referencia circular]");
            return;
        }
        texto.append(fallo.getClass().getName());
        if (fallo.getMessage() != null && !fallo.getMessage().isBlank()) {
            texto.append(": ").append(fallo.getMessage());
        }
        for (Throwable suprimido : fallo.getSuppressed()) {
            texto.append(" suprimida={");
            agregarThrowable(texto, suprimido, visitados);
            texto.append('}');
        }
        if (fallo.getCause() != null) {
            texto.append(" causa={");
            agregarThrowable(texto, fallo.getCause(), visitados);
            texto.append('}');
        }
    }
}
