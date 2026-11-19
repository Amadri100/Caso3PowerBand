package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.Constantes;

public final class ComandoGrabado {

    private final long tiempoRelativoMillis;
    private final IdSintetizador idSintetizador;
    private final TipoAccion tipoAccion;
    private final double[] parametros;

    public ComandoGrabado(long tiempoRelativoMillis,
            IdSintetizador idSintetizador, TipoAccion tipoAccion,
            double[] parametros) {
        if (tiempoRelativoMillis < 0) {
            throw new IllegalArgumentException(
                    "El tiempo relativo no puede ser negativo");
        }
        this.tiempoRelativoMillis = tiempoRelativoMillis;
        this.idSintetizador = Objects.requireNonNull(
                idSintetizador, "idSintetizador");
        this.tipoAccion = Objects.requireNonNull(tipoAccion, "tipoAccion");
        Objects.requireNonNull(parametros, "parametros");
        if (parametros.length > Constantes.MAXIMO_PARAMETROS_COMANDO) {
            rechazarCantidadParametros(parametros.length);
        }
        this.parametros = parametros.clone();
    }

    public long tiempoRelativoMillis() {
        return tiempoRelativoMillis;
    }

    public IdSintetizador idSintetizador() {
        return idSintetizador;
    }

    public TipoAccion tipoAccion() {
        return tipoAccion;
    }

    public double[] parametros() {
        return parametros.clone();
    }

    public byte[] serializar() {
        ByteBuffer buffer = ByteBuffer.allocate(
                Constantes.TAMANO_COMANDO_GRABADO)
                .order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(tiempoRelativoMillis);
        buffer.put((byte) idSintetizador.codigo());
        buffer.put((byte) tipoAccion.codigo());
        buffer.put((byte) parametros.length);
        for (int indice = 0;
                indice < Constantes.MAXIMO_PARAMETROS_COMANDO; indice++) {
            buffer.putDouble(indice < parametros.length
                    ? parametros[indice] : 0.0);
        }
        return buffer.array();
    }

    public static ComandoGrabado desdeBytes(byte[] datos) {
        Objects.requireNonNull(datos, "datos");
        if (datos.length != Constantes.TAMANO_COMANDO_GRABADO) {
            throw new IllegalArgumentException(
                    "El comando grabado debe tener "
                            + Constantes.TAMANO_COMANDO_GRABADO
                            + " bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);
        long tiempoRelativoMillis = buffer.getLong();
        int codigoSintetizador = Byte.toUnsignedInt(buffer.get());
        int codigoAccion = Byte.toUnsignedInt(buffer.get());
        int cantidadParametros = Byte.toUnsignedInt(buffer.get());
        if (cantidadParametros > Constantes.MAXIMO_PARAMETROS_COMANDO) {
            rechazarCantidadParametros(cantidadParametros);
        }

        double[] parametros = new double[cantidadParametros];
        for (int indice = 0;
                indice < Constantes.MAXIMO_PARAMETROS_COMANDO; indice++) {
            double parametro = buffer.getDouble();
            if (indice < cantidadParametros) {
                parametros[indice] = parametro;
            } else if (parametro != 0.0) {
                throw new IllegalArgumentException(
                        "Los parametros no utilizados deben ser cero");
            }
        }

        IdSintetizador idSintetizador =
                IdSintetizador.desdeCodigo(codigoSintetizador);
        TipoAccion tipoAccion = TipoAccion.desdeCodigo(codigoAccion);
        return new ComandoGrabado(tiempoRelativoMillis,
                idSintetizador, tipoAccion, parametros);
    }

    private static void rechazarCantidadParametros(int cantidad) {
        String mensaje = "Cantidad de parametros fuera del maximo de "
                + Constantes.MAXIMO_PARAMETROS_COMANDO + ": " + cantidad;
        LoggerPowerBand.getInstance().registrarError(mensaje);
        throw new IllegalArgumentException(mensaje);
    }

    @Override
    public String toString() {
        return "ComandoGrabado{tiempoRelativoMillis=" + tiempoRelativoMillis
                + ", idSintetizador=" + idSintetizador
                + ", tipoAccion=" + tipoAccion
                + ", parametros=" + Arrays.toString(parametros) + '}';
    }
}
