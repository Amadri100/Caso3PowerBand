package powerband.sintetizadores;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import powerband.util.Constantes;

public final class OrganoSintetizador implements Sintetizador {

    private final BandClient bandClient;
    private final Map<Integer, NotaActiva> notasActivas = new HashMap<>();
    private final Supplier<ProgramadorNotas> fabricaProgramador;
    private ProgramadorNotas programador;

    public OrganoSintetizador(BandClient bandClient) {
        this(bandClient, ProgramadorNotasReal::new);
    }

    protected OrganoSintetizador(BandClient bandClient,
                       Supplier<ProgramadorNotas> fabricaProgramador) {
        this.bandClient = Objects.requireNonNull(bandClient, "bandClient");
        this.fabricaProgramador = Objects.requireNonNull(
                fabricaProgramador, "fabricaProgramador");
    }

    @Override
    public IdSintetizador id() {
        return IdSintetizador.ORGANO;
    }

    @Override
    public BandClient bandClient() {
        return bandClient;
    }

    @Override
    public synchronized void ejecutarPuntual(double... parametros) {
        iniciarNotaPuntual(parametros);
    }

    @Override
    public synchronized void ejecutarPuntual(
            ParametrosNota parametros) {
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.duracionMs() == null) {
            throw new IllegalArgumentException(
                    "Una ejecución puntual requiere duración");
        }
        iniciarNotaPuntual(new double[] {
                parametros.notaMidi(),
                parametros.duracionMs()
        });
    }

    @Override
    public synchronized void iniciar(String voiceId,
                                     double... parametros) {
        validarIdentificadorVoz(voiceId);
        boolean personalizada = parametros != null
                && parametros.length == 1 + 2
                        * Constantes.CANTIDAD_PARCIALES_ORGANO;
        if (!personalizada && (parametros == null
                || parametros.length != 1)) {
            rechazar("El inicio del organo requiere una nota y, "
                    + "opcionalmente, tres razones y tres amplitudes");
        }
        int nota = notaMidi(parametros[0], "organo");
        float[] razones = new float[] {
                (float) Constantes.RAZON_ORGANO_1,
                (float) Constantes.RAZON_ORGANO_2,
                (float) Constantes.RAZON_ORGANO_3
        };
        float[] amplitudes = new float[] {
                (float) Constantes.AMPLITUD_ORGANO_1,
                (float) Constantes.AMPLITUD_ORGANO_2,
                (float) Constantes.AMPLITUD_ORGANO_3
        };
        if (personalizada) {
            for (int indice = 0;
                    indice < Constantes.CANTIDAD_PARCIALES_ORGANO;
                    indice++) {
                razones[indice] = razonParcial(
                        parametros[indice + 1], indice + 1);
                amplitudes[indice] = amplitudParcial(
                        parametros[indice + 1
                                + Constantes.CANTIDAD_PARCIALES_ORGANO],
                        indice + 1);
            }
        }
        enviar("iniciar la nota sostenida del organo",
                () -> bandClient.startOrgan(
                        voiceId, nota, razones, amplitudes));
    }

    @Override
    public synchronized void iniciar(
            String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        float[] razones = new float[] {
                (float) Constantes.RAZON_ORGANO_1,
                (float) Constantes.RAZON_ORGANO_2,
                (float) Constantes.RAZON_ORGANO_3
        };
        float[] amplitudes = new float[] {
                (float) parametros.amplitud(),
                (float) Constantes.AMPLITUD_ORGANO_2,
                (float) Constantes.AMPLITUD_ORGANO_3
        };
        enviar("iniciar la nota sostenida del organo",
                () -> bandClient.startOrgan(
                        voiceId,
                        parametros.notaMidi(),
                        razones,
                        amplitudes));
    }

    @Override
    public synchronized void actualizar(String voiceId,
                                        double... parametros) {
        validarIdentificadorVoz(voiceId);
        validarCantidad(parametros, 1, 1, "organo");
        int notaNueva = notaMidi(parametros[0], "organo");
        enviar("actualizar la nota del organo",
                () -> bandClient.setOrganNote(voiceId, notaNueva));
    }

    @Override
    public synchronized void actualizar(
            String voiceId, ParametrosNota parametros) {
        validarIdentificadorVoz(voiceId);
        Objects.requireNonNull(parametros, "parametros");
        enviar("actualizar la nota del organo",
                () -> bandClient.setOrganNote(
                        voiceId, parametros.notaMidi()));
        enviar("actualizar la amplitud del organo",
                () -> bandClient.setOrganAmplitude(
                        voiceId, (float) parametros.amplitud()));
    }

    @Override
    public synchronized void finalizar(String voiceId,
                                       double... parametros) {
        validarIdentificadorVoz(voiceId);
        enviar("finalizar la nota del organo",
                () -> bandClient.stopOrgan(voiceId));
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
        try {
            enviar("liberar todas las voces del organo",
                    () -> bandClient.clearSynth("organo"));
        } catch (IllegalStateException fallo) {
            if (falloTotal == null) {
                falloTotal = fallo;
            } else {
                falloTotal.addSuppressed(fallo);
            }
        }
        if (falloTotal != null) {
            throw falloTotal;
        }
    }

    private void validarIdentificadorVoz(String voiceId) {
        if (voiceId == null || voiceId.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador de voz no puede estar vacio");
        }
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
