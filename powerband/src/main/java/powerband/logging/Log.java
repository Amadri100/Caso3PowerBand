package powerband.logging;

import java.time.Instant;
import java.util.Objects;

import powerband.util.Configuraciones;
import powerband.util.Constantes;

public final class Log {

    private final Instant momento;
    private final CategoriaLog categoria;
    private final String mensaje;
    private final DebugInfo debugInfo;

    public Log(Instant momento, CategoriaLog categoria, String mensaje, DebugInfo debugInfo) {
        this.momento = Objects.requireNonNull(momento, "momento");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.mensaje = Objects.requireNonNull(mensaje, "mensaje");
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El evento no puede estar vacio");
        }
        this.debugInfo = debugInfo;
    }

    public static Log crear(CategoriaLog categoria, Object mensaje,
            OrigenLog origen, Throwable throwable) {
        Objects.requireNonNull(mensaje, "mensaje");
        return new Log(Instant.now(), categoria, mensaje.toString(),
                origen == null ? null : new DebugInfo(origen, throwable));
    }

    public static Log comando(Object mensaje, OrigenLog origen) {
        return crear(CategoriaLog.COMANDO, mensaje, origen, null);
    }

    public static Log info(Object mensaje, OrigenLog origen) {
        return crear(CategoriaLog.INFO, mensaje, origen, null);
    }

    public static Log error(Object mensaje, OrigenLog origen, Throwable throwable) {
        if (throwable != null && (mensaje == null || mensaje.toString().isBlank())) {
            mensaje = throwable.getClass().getName();
        }
        return crear(CategoriaLog.ERROR, mensaje, origen, throwable);
    }

    public static Log errorArchivo(Object mensaje, String ubicacion,
            OrigenLog origen, Throwable throwable) {
        String texto = throwable != null && (mensaje == null || mensaje.toString().isBlank())
                ? throwable.getClass().getName()
                : Objects.requireNonNull(mensaje, "mensaje").toString();
        if (texto.isBlank()) {
            throw new IllegalArgumentException("El evento no puede estar vacio");
        }
        if (ubicacion != null && !ubicacion.isBlank()) {
            texto += " ubicacion=" + ubicacion;
        }
        return error(texto, origen, throwable);
    }

    protected Throwable throwable() {
        return debugInfo == null ? null : debugInfo.throwable();
    }

    @Override
    public String toString() {
        StringBuilder texto = new StringBuilder()
                .append(momento).append(" [").append(categoria.texto()).append("] ")
                .append(mensaje.replace("\r", "\\r").replace("\n", "\\n").strip());
        if (debugInfo != null && Boolean.TRUE.equals(
                Configuraciones.getInstance().getOpcion(Constantes.OPCION_DEBUG_LOG))) {
            texto.append(' ').append(debugInfo);
        }
        return texto.toString().replace("\r", "\\r").replace("\n", "\\n");
    }
}
