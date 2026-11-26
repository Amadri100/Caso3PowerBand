package powerband.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Configuraciones {

    private static final Configuraciones INSTANCIA = new Configuraciones();

    private String host;
    private int puerto;
    private final Map<String, Object> opcionesModificables;

    public Configuraciones() {
        this.host = Constantes.HOST_DEFECTO;
        this.puerto = Constantes.PUERTO_DEFECTO;
        this.opcionesModificables = new LinkedHashMap<>();
        this.opcionesModificables.put(Constantes.OPCION_DEBUG_LOG, Boolean.FALSE);
    }

    public static Configuraciones getInstance() {
        return INSTANCIA;
    }

    public static Configuraciones obtenerInstancia() {
        return getInstance();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        Objects.requireNonNull(host, "host");
        if (host.isBlank()) {
            throw new IllegalArgumentException("El host no puede estar vacio");
        }
        this.host = host;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        if (puerto < 1 || puerto > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
        }
        this.puerto = puerto;
    }

    public synchronized Object getOpcion(String clave) {
        return opcionesModificables.get(requireClave(clave));
    }

    public synchronized void setOpcion(String clave, Object valor) {
        if (Constantes.OPCION_DEBUG_LOG.equals(clave) && !(valor instanceof Boolean)) {
            throw new IllegalArgumentException("La opción de depuración debe ser booleana");
        }
        opcionesModificables.put(requireClave(clave), valor);
    }

    public synchronized void eliminarOpcion(String clave) {
        if (Constantes.OPCION_DEBUG_LOG.equals(clave)) {
            throw new IllegalArgumentException("No se puede eliminar la opción de depuración");
        }
        opcionesModificables.remove(requireClave(clave));
    }

    public synchronized Map<String, Object> getOpcionesModificables() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(opcionesModificables));
    }

    private static String requireClave(String clave) {
        Objects.requireNonNull(clave, "clave");
        if (clave.isBlank()) {
            throw new IllegalArgumentException("La clave no puede estar vacia");
        }
        return clave;
    }
}
