package powerband.sintetizadores;

import java.util.Objects;

import powerband.util.Constantes;

public record ParametrosNota(
        int notaMidi,
        Integer duracionMs,
        double amplitud) {

    public ParametrosNota {
        if (notaMidi < Constantes.NOTA_MIDI_MINIMA
                || notaMidi > Constantes.NOTA_MIDI_MAXIMA) {
            throw new IllegalArgumentException(
                    "La nota MIDI debe estar entre 0 y 127");
        }
        if (duracionMs != null
                && (duracionMs < Constantes.DURACION_MINIMA_MS
                        || duracionMs > Constantes.DURACION_MAXIMA_MS)) {
            throw new IllegalArgumentException(
                    "La duración debe estar dentro del rango permitido");
        }
        if (!Double.isFinite(amplitud)
                || amplitud < Constantes.AMPLITUD_MINIMA
                || amplitud > Constantes.AMPLITUD_MAXIMA) {
            throw new IllegalArgumentException(
                    "La amplitud debe estar entre 0 y 1");
        }
    }

    public ParametrosNota(int notaMidi) {
        this(notaMidi, null, Constantes.AMPLITUD_GENERAL_DEFECTO);
    }

    public ParametrosNota(int notaMidi, int duracionMs) {
        this(notaMidi, duracionMs, Constantes.AMPLITUD_GENERAL_DEFECTO);
    }

    public double[] parametrosPuntuales() {
        if (duracionMs == null) {
            return new double[] {notaMidi, amplitud};
        }
        return new double[] {notaMidi, duracionMs, amplitud};
    }

    public double[] parametrosSostenidos() {
        return new double[] {notaMidi, amplitud};
    }

    public static ParametrosNota desdeParametrosPuntuales(
            double[] parametros) {
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.length < 2) {
            throw new IllegalArgumentException(
                    "Se requieren nota MIDI y duración");
        }
        int notaMidi = entero(parametros[0], "nota MIDI");
        int duracionMs = entero(parametros[1], "duración");
        double amplitud = Constantes.AMPLITUD_GENERAL_DEFECTO;
        return new ParametrosNota(notaMidi, duracionMs, amplitud);
    }

    public static ParametrosNota desdeParametrosSostenidos(
            double[] parametros) {
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.length < 1) {
            throw new IllegalArgumentException(
                    "Se requiere una nota MIDI");
        }
        int notaMidi = entero(parametros[0], "nota MIDI");
        double amplitud = parametros.length >= 2
                ? parametros[1]
                : Constantes.AMPLITUD_GENERAL_DEFECTO;
        return new ParametrosNota(notaMidi, null, amplitud);
    }

    private static int entero(double valor, String nombre) {
        if (!Double.isFinite(valor) || valor != Math.rint(valor)) {
            throw new IllegalArgumentException(
                    nombre + " debe ser un entero");
        }
        return (int) valor;
    }
}
