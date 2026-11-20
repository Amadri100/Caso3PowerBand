package powerband.sintetizadores;

import java.util.HashSet;
import java.util.Set;

import powerband.comandos.TipoAccion;
import powerband.util.Constantes;

public final class OrganoSintetizador extends AdaptadorBandClient {

    private final Set<Integer> notasActivas = new HashSet<>();

    public OrganoSintetizador(BandClient bandClient) {
        super(bandClient);
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
        } else {
            rechazar("El organo solo admite inicio y finalizacion de notas");
        }
    }

    @Override
    public synchronized void liberarTodasLasNotas() {
        IllegalStateException falloTotal = null;
        for (int nota : Set.copyOf(notasActivas)) {
            try {
                enviar("finalizar la nota " + nota + " del organo",
                        () -> bandClient.noteOff(nota));
                notasActivas.remove(nota);
            } catch (IllegalStateException fallo) {
                if (falloTotal == null) {
                    falloTotal = fallo;
                } else {
                    falloTotal.addSuppressed(fallo);
                }
            }
        }
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

        float[] razonesEnvio = razones;
        float[] amplitudesEnvio = amplitudes;
        if (notasActivas.contains(nota)) {
            enviar("finalizar la nota previa " + nota + " del organo",
                    () -> bandClient.noteOff(nota));
            notasActivas.remove(nota);
        }
        enviar("iniciar la nota " + nota + " del organo",
                () -> bandClient.noteOn(nota, razonesEnvio, amplitudesEnvio));
        notasActivas.add(nota);
    }

    private void finalizarNota(double[] parametros) {
        validarCantidad(parametros, 1, 1, "organo");
        int nota = notaMidi(parametros[0], "organo");
        if (notasActivas.contains(nota)) {
            enviar("finalizar la nota " + nota + " del organo",
                    () -> bandClient.noteOff(nota));
            notasActivas.remove(nota);
        }
    }
}
