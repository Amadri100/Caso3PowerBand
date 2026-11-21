package powerband.grabaciones;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;

import powerband.archivos.ComandoGrabado;
import powerband.comandos.Comando;
import powerband.comandos.TipoAccion;
import powerband.util.Constantes;

public final class Reproductor {

    private final Function<ComandoGrabado, Comando> reconstruir;
    private final LongSupplier relojMonotonico;
    private final EsperaReproduccion espera;
    private final Object bloqueoControl = new Object();
    private Thread hiloReproduccion;
    private Thread hiloEnEspera;
    private volatile boolean detencionSolicitada;

    public Reproductor(
            Function<ComandoGrabado, Comando> reconstruir,
            LongSupplier relojMonotonico,
            EsperaReproduccion espera) {
        this.reconstruir = Objects.requireNonNull(
                reconstruir, "reconstruir");
        this.relojMonotonico = Objects.requireNonNull(
                relojMonotonico, "relojMonotonico");
        this.espera = Objects.requireNonNull(espera, "espera");
    }

    public void reproducir(GrabacionAbierta grabacion)
            throws IOException, InterruptedException {
        Objects.requireNonNull(grabacion, "grabacion");
        try (GrabacionAbierta lectura = grabacion) {
            iniciarControl();
            try {
                reproducirLectura(lectura);
            } finally {
                terminarControl();
            }
        }
    }
    //Detiene el hilos
    public boolean detener() {
        Thread hiloAInterrumpir;
        synchronized (bloqueoControl) {
            if (hiloReproduccion == null) {
                return false;
            }
            detencionSolicitada = true;
            hiloAInterrumpir = hiloEnEspera;
        }
        if (hiloAInterrumpir != null) {
            hiloAInterrumpir.interrupt();
        }
        return true;
    }
    //Lee una grabacion abierta, 
    private void reproducirLectura(GrabacionAbierta lectura)
            throws IOException, InterruptedException {
        long inicioNanos = relojMonotonico.getAsLong();
        //Crea una cola 
        Deque<ComandoGrabado> buffer = new ArrayDeque<>(
                Constantes.CAPACIDAD_BUFFER_REPRODUCCION);
        List<Comando> notasActivas = new ArrayList<>();
        boolean finLectura = false;
        Throwable falloPrincipal = null;

        try {
            while (!finLectura || !buffer.isEmpty()) {
                if (detencionSolicitada) {
                    break;
                }
                //Agrega comandos al buffer si puede
                while (!finLectura && !detencionSolicitada
                        && buffer.size()
                                < Constantes.CAPACIDAD_BUFFER_REPRODUCCION) {
                    Optional<ComandoGrabado> siguiente =
                            lectura.siguienteComando();
                    if (siguiente.isEmpty()) {
                        finLectura = true;
                    } else {
                        buffer.addLast(siguiente.orElseThrow());
                    }
                }
                //Toma elementos del buffer
                while (!buffer.isEmpty()) {
                    if (detencionSolicitada) {
                        break;
                    }
                    ComandoGrabado grabado = buffer.removeFirst();
                    //Espera el momento para ejecutar el comando
                    if (!esperarHasta(inicioNanos,
                            convertirTiempoANanos(
                                    grabado.tiempoRelativoMillis()))) {
                        break;
                    }
                    //Si se pide detener para aqui
                    if (detencionSolicitada) {
                        break;
                    }
                    //Reconstruye el comando
                    Comando comando = Objects.requireNonNull(
                            reconstruir.apply(grabado),
                            "La reconstrucción devolvió null");
                    validarComandoSostenido(comando);
                    if (detencionSolicitada) {
                        break;
                    }
                    //Ejecuta el comando en el sinteitzador
                    comando.ejecutar();
                    actualizarNotasActivas(comando, notasActivas);
                }
            }
        } catch (IOException | InterruptedException | RuntimeException
                | Error fallo) {
            falloPrincipal = fallo;
            throw fallo;
        } finally {
            RuntimeException falloFinalizacion =
                    finalizarNotasActivas(notasActivas);
            if (falloFinalizacion != null) {
                if (falloPrincipal == null) {
                    throw falloFinalizacion;
                }
                if (falloPrincipal != falloFinalizacion) {
                    falloPrincipal.addSuppressed(falloFinalizacion);
                }
            }
        }
    }

    private boolean esperarHasta(long inicioNanos, long plazoNanos)
            throws InterruptedException {
        while (true) {
            if (detencionSolicitada) {
                return false;
            }
            if (Thread.interrupted()) {
                throw new InterruptedException(
                        "La reproducción fue interrumpida");
            }
            long transcurridoNanos =
                    relojMonotonico.getAsLong() - inicioNanos;
            if (transcurridoNanos < 0) {
                throw new IllegalStateException(
                        "El reloj monotónico retrocedió durante la reproducción");
            }
            if (transcurridoNanos >= plazoNanos) {
                return true;
            }
            Thread hiloActual = Thread.currentThread();
            synchronized (bloqueoControl) {
                if (detencionSolicitada) {
                    return false;
                }
                hiloEnEspera = hiloActual;
            }
            try {
                //Hace que el programa espere, no utiliza sleep
                espera.esperar(plazoNanos - transcurridoNanos);
            } catch (InterruptedException excepcion) {
                if (detencionSolicitada) {
                    return false;
                }
                throw excepcion;
            } finally {
                synchronized (bloqueoControl) {
                    if (hiloEnEspera == hiloActual) {
                        hiloEnEspera = null;
                    }
                }
            }
        }
    }
    //Asigna el thread
    private void iniciarControl() {
        synchronized (bloqueoControl) {
            if (hiloReproduccion != null) {
                throw new IllegalStateException(
                        "Ya hay una reproducción en curso");
            }
            detencionSolicitada = false;
            hiloReproduccion = Thread.currentThread();
        }
    }
    //Termina los hilos
    private void terminarControl() {
        synchronized (bloqueoControl) {
            if (hiloReproduccion == Thread.currentThread()
                    && detencionSolicitada) {
                Thread.interrupted();
            }
            hiloEnEspera = null;
            hiloReproduccion = null;
            detencionSolicitada = false;
        }
    }
    
    private static void validarComandoSostenido(Comando comando) {
        if (comando.tipoAccion() == TipoAccion.INICIO
                && comando.parametros().length == 0) {
            throw new IllegalArgumentException(
                    "Un comando de inicio requiere identificar la nota");
        }
    }
    //Actualiza la lista de notas activas
    private static void actualizarNotasActivas(Comando comando,
            List<Comando> notasActivas) {
        double[] parametros = comando.parametros();
        //Si se esta iniciando se agrega el comando
        if (comando.tipoAccion() == TipoAccion.INICIO) {
            notasActivas.add(comando);
        } else if (comando.tipoAccion() == TipoAccion.FINALIZACION
                && parametros.length > 0) {
            long notaFinalizada = Double.doubleToLongBits(parametros[0]);
            for (int indice = notasActivas.size() - 1;
                    indice >= 0; indice--) {
                Comando inicio = notasActivas.get(indice);
                double[] parametrosInicio = inicio.parametros();
                //Si la nota finalizo se elimina
                if (inicio.destino().id() == comando.destino().id()
                        && Double.doubleToLongBits(parametrosInicio[0])
                                == notaFinalizada) {
                    notasActivas.remove(indice);
                    break;
                }
            }
        }
    }

    private static RuntimeException finalizarNotasActivas(
            List<Comando> notasActivas) {
        RuntimeException falloPrincipal = null;
        for (int indice = notasActivas.size() - 1;
                indice >= 0; indice--) {
            Comando inicio = notasActivas.get(indice);
            try {
                Comando finalizacion = new ComandoFinalizacion(
                        inicio.destino(), inicio.parametros()[0]);
                finalizacion.ejecutar();
            } catch (RuntimeException fallo) {
                if (falloPrincipal == null) {
                    falloPrincipal = fallo;
                } else if (falloPrincipal != fallo) {
                    falloPrincipal.addSuppressed(fallo);
                }
            }
        }
        return falloPrincipal;
    }

    private static long convertirTiempoANanos(long tiempoRelativoMillis) {
        try {
            return Duration.ofMillis(tiempoRelativoMillis).toNanos();
        } catch (ArithmeticException excepcion) {
            throw new IllegalArgumentException(
                    "El tiempo relativo no puede representarse en nanosegundos: "
                            + tiempoRelativoMillis,
                    excepcion);
        }
    }
}
