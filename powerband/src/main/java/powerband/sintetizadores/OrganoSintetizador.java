package powerband.sintetizadores;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import powerband.comandos.TipoAccion;
import powerband.util.Constantes;

public final class OrganoSintetizador extends AdaptadorBandClient {

    private final Map<Integer, NotaActiva> notasActivas = new HashMap<>();
    private final Supplier<ProgramadorNotas> fabricaProgramador;
    private ProgramadorNotas programador;

    public OrganoSintetizador(BandClient bandClient) {
        this(bandClient, ProgramadorNotasReal::new);
    }

    OrganoSintetizador(BandClient bandClient,
                       Supplier<ProgramadorNotas> fabricaProgramador) {
        super(bandClient);
        this.fabricaProgramador = Objects.requireNonNull(
                fabricaProgramador, "fabricaProgramador");
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.ORGANO;
    }

    @Override
    public synchronized void ejecutar(
            TipoAccion tipoAccion, double... parametros) {
        if (tipoAccion == TipoAccion.INICIO) {
            iniciarNota(parametros);
        } else if (tipoAccion == TipoAccion.FINALIZACION) {
            finalizarNota(parametros);
        } else if (tipoAccion == TipoAccion.PUNTUAL) {
            iniciarNotaPuntual(parametros);
        } else {
            rechazar("El organo no admite la accion " + tipoAccion);
        }
    }

    @Override
    public synchronized void liberarTodasLasNotas() {
        IllegalStateException falloTotal = null;
        for (Map.Entry<Integer, NotaActiva> entrada
                : Map.copyOf(notasActivas).entrySet()) {
            int nota = entrada.getKey();
            NotaActiva activa = entrada.getValue();
            cancelarFinalizacion(activa);
            try {
                enviar("finalizar la nota " + nota + " del organo",
                        () -> bandClient.noteOff(nota));
                notasActivas.remove(nota, activa);
            } catch (IllegalStateException fallo) {
                if (falloTotal == null) {
                    falloTotal = fallo;
                } else {
                    falloTotal.addSuppressed(fallo);
                }
            }
        }
        cerrarProgramadorSiNoHayFinalizaciones();
        if (falloTotal != null) {
            throw falloTotal;
        }
    }

    private void iniciarNota(double[] parametros) {
        boolean personalizada = parametros != null
                && parametros.length == 1 + 2 * Constantes.CANTIDAD_PARCIALES_ORGANO;
        if (!personalizada && (parametros == null || parametros.length != 1)) {
            rechazar("El inicio del organo requiere una nota y, opcionalmente, "
                    + "tres razones y tres amplitudes");
        }

        int nota = notaMidi(parametros[0], "organo");
        float[] razones = new float[Constantes.CANTIDAD_PARCIALES_ORGANO];
        float[] amplitudes = new float[Constantes.CANTIDAD_PARCIALES_ORGANO];
        if (personalizada) {
            for (int indice = 0; indice < Constantes.CANTIDAD_PARCIALES_ORGANO; indice++) {
                razones[indice] = razonParcial(parametros[indice + 1], indice + 1);
                amplitudes[indice] = amplitudParcial(
                        parametros[indice + 1 + Constantes.CANTIDAD_PARCIALES_ORGANO],
                        indice + 1);
            }
        } else {
            razones = new float[] {
                    (float) Constantes.RAZON_ORGANO_1,
                    (float) Constantes.RAZON_ORGANO_2,
                    (float) Constantes.RAZON_ORGANO_3
            };
            amplitudes = new float[] {
                    (float) Constantes.AMPLITUD_ORGANO_1,
                    (float) Constantes.AMPLITUD_ORGANO_2,
                    (float) Constantes.AMPLITUD_ORGANO_3
            };
        }

        iniciarNota(nota, razones, amplitudes, null);
    }

    private void iniciarNotaPuntual(double[] parametros) {
        validarCantidad(parametros, 2, 2, "organo");
        int nota = notaMidi(parametros[0], "organo");
        int duracion = duracionMs(parametros[1], "organo");
        float[] razones = {
                (float) Constantes.RAZON_ORGANO_1,
                (float) Constantes.RAZON_ORGANO_2,
                (float) Constantes.RAZON_ORGANO_3
        };
        float[] amplitudes = {
                (float) Constantes.AMPLITUD_ORGANO_1,
                (float) Constantes.AMPLITUD_ORGANO_2,
                (float) Constantes.AMPLITUD_ORGANO_3
        };
        iniciarNota(nota, razones, amplitudes, duracion);
    }

    private void iniciarNota(int nota, float[] razones, float[] amplitudes,
                             Integer duracionMs) {
        NotaActiva anterior = notasActivas.get(nota);
        if (anterior != null) {
            enviar("finalizar la nota previa " + nota + " del organo",
                    () -> bandClient.noteOff(nota));
            cancelarFinalizacion(anterior);
            notasActivas.remove(nota, anterior);
        }
        enviar("iniciar la nota " + nota + " del organo",
                () -> bandClient.noteOn(nota, razones, amplitudes));
        NotaActiva activa = new NotaActiva();
        notasActivas.put(nota, activa);
        if (duracionMs != null) {
            programarFinalizacion(nota, activa, duracionMs);
        }
    }

    private void finalizarNota(double[] parametros) {
        validarCantidad(parametros, 1, 1, "organo");
        int nota = notaMidi(parametros[0], "organo");
        NotaActiva activa = notasActivas.get(nota);
        if (activa != null) {
            enviar("finalizar la nota " + nota + " del organo",
                    () -> bandClient.noteOff(nota));
            cancelarFinalizacion(activa);
            notasActivas.remove(nota, activa);
            cerrarProgramadorSiNoHayFinalizaciones();
        }
    }

    private void programarFinalizacion(int nota, NotaActiva activa,
                                       int duracionMs) {
        try {
            if (programador == null) {
                programador = Objects.requireNonNull(
                        fabricaProgramador.get(), "programador");
            }
            activa.finalizacion = Objects.requireNonNull(
                    programador.programar(
                            () -> finalizarNotaTemporizada(nota, activa),
                            duracionMs),
                    "finalizacion programada");
        } catch (RuntimeException falloProgramacion) {
            IllegalStateException fallo = new IllegalStateException(
                    "No se pudo programar la finalizacion de la nota "
                            + nota + " del organo",
                    falloProgramacion);
            try {
                enviar("finalizar la nota " + nota
                                + " tras fallar su programacion",
                        () -> bandClient.noteOff(nota));
                notasActivas.remove(nota, activa);
            } catch (IllegalStateException falloFinalizacion) {
                fallo.addSuppressed(falloFinalizacion);
            } finally {
                cerrarProgramadorSiNoHayFinalizaciones();
            }
            registrarError(fallo);
            throw fallo;
        }
    }

    private synchronized void finalizarNotaTemporizada(
            int nota, NotaActiva activa) {
        if (notasActivas.get(nota) != activa
                || activa.finalizacion == null) {
            return;
        }
        activa.finalizacion = null;
        try {
            enviar("finalizar la nota " + nota + " del organo",
                    () -> bandClient.noteOff(nota));
            notasActivas.remove(nota, activa);
        } finally {
            cerrarProgramadorSiNoHayFinalizaciones();
        }
    }

    private void cancelarFinalizacion(NotaActiva activa) {
        TareaProgramada finalizacion = activa.finalizacion;
        activa.finalizacion = null;
        if (finalizacion != null) {
            finalizacion.cancelar();
        }
    }

    private void cerrarProgramadorSiNoHayFinalizaciones() {
        boolean hayFinalizaciones = notasActivas.values().stream()
                .anyMatch(activa -> activa.finalizacion != null);
        if (!hayFinalizaciones && programador != null) {
            programador.cerrar();
            programador = null;
        }
    }

    private static final class NotaActiva {
        private TareaProgramada finalizacion;
    }

    interface ProgramadorNotas {
        TareaProgramada programar(Runnable tarea, long demoraMs);

        void cerrar();
    }

    interface TareaProgramada {
        void cancelar();
    }

    private static final class ProgramadorNotasReal
            implements ProgramadorNotas {

        private final ScheduledThreadPoolExecutor ejecutor;

        private ProgramadorNotasReal() {
            ThreadFactory fabricaHilos = tarea -> {
                Thread hilo = new Thread(
                        tarea, Constantes.NOMBRE_HILO_NOTAS_ORGANO);
                hilo.setDaemon(true);
                return hilo;
            };
            ejecutor = new ScheduledThreadPoolExecutor(1, fabricaHilos);
            ejecutor.setRemoveOnCancelPolicy(true);
        }

        @Override
        public TareaProgramada programar(Runnable tarea, long demoraMs) {
            var vencimiento = ejecutor.schedule(
                    tarea, demoraMs, TimeUnit.MILLISECONDS);
            return () -> vencimiento.cancel(false);
        }

        @Override
        public void cerrar() {
            ejecutor.shutdown();
        }
    }
}
