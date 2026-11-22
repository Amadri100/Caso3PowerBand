package powerband.input;

public final class NormalizadorEntrada {

    private NormalizadorEntrada() {
    }

    public static double normalizar(double valor,
                                    double origenMinimo,
                                    double origenMaximo,
                                    double destinoMinimo,
                                    double destinoMaximo) {
        validarLimites(origenMinimo, origenMaximo,
                destinoMinimo, destinoMaximo);
        if (!Double.isFinite(valor)
                || valor < origenMinimo
                || valor > origenMaximo) {
            throw new IllegalArgumentException(
                    "Valor fuera del rango de entrada: " + valor);
        }

        double proporcion = (valor - origenMinimo)
                / (origenMaximo - origenMinimo);
        return destinoMinimo
                + proporcion * (destinoMaximo - destinoMinimo);
    }

    public static double normalizarCeroUno(double valor,
                                           double origenMinimo,
                                           double origenMaximo) {
        return normalizar(valor, origenMinimo, origenMaximo, 0.0, 1.0);
    }

    public static boolean estaEnRango(double valor,
                                      double minimo,
                                      double maximo) {
        return Double.isFinite(valor) && valor >= minimo && valor <= maximo;
    }

    public double normalizarValor(double valor,
                                  double origenMinimo,
                                  double origenMaximo,
                                  double destinoMinimo,
                                  double destinoMaximo) {
        return normalizar(valor, origenMinimo, origenMaximo,
                destinoMinimo, destinoMaximo);
    }

    private static void validarLimites(double origenMinimo,
                                       double origenMaximo,
                                       double destinoMinimo,
                                       double destinoMaximo) {
        if (!Double.isFinite(origenMinimo)
                || !Double.isFinite(origenMaximo)
                || !Double.isFinite(destinoMinimo)
                || !Double.isFinite(destinoMaximo)
                || origenMinimo >= origenMaximo) {
            throw new IllegalArgumentException(
                    "Los rangos de normalizacion no son validos");
        }
    }
}
