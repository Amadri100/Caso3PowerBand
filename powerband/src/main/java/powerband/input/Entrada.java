package powerband.input;

import java.util.Objects;
import java.util.function.Consumer;

import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;

public abstract class Entrada implements AutoCloseable {

    private final String nombre;
    private volatile boolean activa;
    private volatile Thread hilo;
    private volatile Consumer<Entrada> callbackDesconexion;
    private volatile Runnable callbackRunnable;
    private volatile ObservadorDesconexion observadorDesconexion;

    protected Entrada(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    protected Entrada(String nombre, Consumer<Entrada> callbackDesconexion) {
        this.nombre = validarNombre(nombre);
        this.callbackDesconexion = callbackDesconexion;
    }

    protected Entrada(String nombre, Runnable callbackRunnable) {
        this.nombre = validarNombre(nombre);
        this.callbackRunnable = callbackRunnable;
    }

    public String nombre() {
        return nombre;
    }

    public synchronized void iniciar() {
        if (activa) {
            return;
        }
        activa = true;
        hilo = new Thread(this::ejecutarHilo,
                "entrada-" + nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        Thread hiloActual;
        synchronized (this) {
            activa = false;
            hiloActual = hilo;
            if (hiloActual != null) {
                hiloActual.interrupt();
            }
        }
        RuntimeException fallo = null;
        try {
            liberarRecursos();
        } catch (RuntimeException excepcion) {
            fallo = excepcion;
        }

        boolean interrumpido = false;
        if (hiloActual != null && hiloActual != Thread.currentThread()) {
            while (hiloActual.isAlive()) {
                try {
                    hiloActual.join();
                } catch (InterruptedException excepcion) {
                    LoggerPowerBand.getInstance().registrarExcepcionNoCapturada(
                            excepcion, OrigenLog.INPUT);
                    interrumpido = true;
                }
            }
        }
        if (interrumpido) {
            InterruptedException excepcion = new InterruptedException(
                    "La espera de finalización de la entrada fue interrumpida");
            if (fallo == null) {
                fallo = new IllegalStateException(
                        excepcion.getMessage(), excepcion);
            } else {
                fallo.addSuppressed(excepcion);
            }
            Thread.currentThread().interrupt();
        }
        if (fallo != null) {
            throw fallo;
        }
    }

    public boolean estaActiva() {
        return activa;
    }

    public boolean activa() {
        return estaActiva();
    }

    public boolean estaIniciada() {
        return estaActiva();
    }

    public void iniciarHilo() {
        iniciar();
    }

    public void detenerHilo() {
        detener();
    }

    public void cambiarCallbackDesconexion(Consumer<Entrada> callback) {
        callbackDesconexion = callback;
    }

    public void registrarCallbackDesconexion(Consumer<Entrada> callback) {
        cambiarCallbackDesconexion(callback);
    }

    public void registrarCallbackDesconexion(Runnable callback) {
        callbackRunnable = callback;
    }

    public void registrarObservadorDesconexion(
            ObservadorDesconexion observador) {
        observadorDesconexion = observador;
    }

    public void desconectar(String motivo) {
        desconectar(motivo, null);
    }

    public void desconectar(String motivo, Throwable causa) {
        Throwable causaNotificacion = causa;
        try {
            detener();
        } catch (RuntimeException falloDetencion) {
            if (causaNotificacion == null) {
                causaNotificacion = falloDetencion;
            } else if (causaNotificacion != falloDetencion) {
                causaNotificacion.addSuppressed(falloDetencion);
            }
        }
        String mensaje = "Desconexion de entrada " + nombre
                + ": " + (motivo == null ? "dispositivo desconectado" : motivo);
        LoggerPowerBand.getInstance().registrarONotificarFallo(
                Log.error(mensaje, OrigenLog.INPUT, causaNotificacion));
        Consumer<Entrada> callback = callbackDesconexion;
        if (callback != null) {
            try {
                callback.accept(this);
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarONotificarFallo(Log.error(
                        "Error al notificar desconexion de " + nombre
                                + ": " + excepcion, OrigenLog.INPUT, excepcion));
            }
        }
        ObservadorDesconexion observador = observadorDesconexion;
        if (observador != null) {
            try {
                observador.entradaDesconectada(
                        this, motivo, causaNotificacion);
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarONotificarFallo(Log.error(
                        "Error al notificar desconexion de " + nombre
                                + ": " + excepcion, OrigenLog.INPUT, excepcion));
            }
        }
        Runnable callbackSimple = callbackRunnable;
        if (callbackSimple != null) {
            try {
                callbackSimple.run();
            } catch (RuntimeException excepcion) {
                LoggerPowerBand.getInstance().registrarONotificarFallo(Log.error(
                        "Error al notificar desconexion de " + nombre
                                + ": " + excepcion, OrigenLog.INPUT, excepcion));
            }
        }
    }

    public void notificarDesconexion(String motivo) {
        desconectar(motivo);
    }

    public void rechazarValor(String descripcion, double valor,
                              double minimo, double maximo) {
        String mensaje = "Valor rechazado en " + nombre + ": "
                + descripcion + "=" + valor
                + " (rango permitido " + minimo + " a " + maximo + ")";
        IllegalArgumentException fallo = new IllegalArgumentException(mensaje);
        LoggerPowerBand.getInstance().registrar(Log.error(mensaje, OrigenLog.INPUT, fallo));
        throw fallo;
    }

    protected void validarValor(String descripcion, double valor,
                                double minimo, double maximo) {
        if (!Double.isFinite(valor) || valor < minimo || valor > maximo) {
            rechazarValor(descripcion, valor, minimo, maximo);
        }
    }

    protected abstract void ejecutarCaptura() throws Exception;

    protected void liberarRecursosEspecificos() {
    }

    public void liberarRecursos() {
        liberarRecursosEspecificos();
    }

    @Override
    public void close() {
        detener();
    }

    private void ejecutarHilo() {
        try {
            ejecutarCaptura();
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException | java.io.IOException excepcion) {
            if (activa) {
                desconectar(excepcion.getMessage(), excepcion);
            } else {
                LoggerPowerBand.getInstance().registrar(Log.error(
                        "Error de captura al detener " + nombre, OrigenLog.INPUT, excepcion));
            }
        } catch (Exception excepcion) {
            if (activa) {
                desconectar(excepcion.getMessage(), excepcion);
            } else {
                LoggerPowerBand.getInstance().registrar(Log.error(
                        "Error de captura al detener " + nombre, OrigenLog.INPUT, excepcion));
            }
        }
    }

    private static String validarNombre(String nombre) {
        Objects.requireNonNull(nombre, "nombre");
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de entrada no puede estar vacio");
        }
        return nombre;
    }
}
