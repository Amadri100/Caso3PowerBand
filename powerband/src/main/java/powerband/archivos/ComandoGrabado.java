package powerband.archivos;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import powerband.comandos.TipoAccion;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.Constantes;
import powerband.excepciones.ArgumentoInvalidoException;
import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.excepciones.ConstantesExcepciones;

public final class ComandoGrabado {

    private final long tiempoRelativoMillis;
    private final IdSintetizador idSintetizador;
    private final TipoAccion tipoAccion;
    private final double[] parametros;

    public ComandoGrabado(long tiempoRelativoMillis,
            IdSintetizador idSintetizador, TipoAccion tipoAccion,
            double[] parametros) {
        if (tiempoRelativoMillis < 0) {
            throw ArgumentoInvalidoException.tiempoRelativoNegativo();
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
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
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
            throw ArgumentoInvalidoException.detalle(
                    TipoErrorArgumento.TAMANO_REGISTRO_INVALIDO,
                    String.format(ConstantesExcepciones.MSG_COMANDO_LONGITUD,
                            Constantes.TAMANO_COMANDO_GRABADO));
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(Constantes.ORDEN_BYTES_PROGRAMA);
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
                throw ArgumentoInvalidoException.parametrosNoUtilizados();
            }
        }

        IdSintetizador idSintetizador =
                IdSintetizador.desdeCodigo(codigoSintetizador);
        TipoAccion tipoAccion = TipoAccion.desdeCodigo(codigoAccion);
        return new ComandoGrabado(tiempoRelativoMillis,
                idSintetizador, tipoAccion, parametros);
    }

    private static void rechazarCantidadParametros(int cantidad) {
        throw ArgumentoInvalidoException.parametrosExceso(
                Constantes.MAXIMO_PARAMETROS_COMANDO, cantidad);
    }

    @Override
    public String toString() {
        return "ComandoGrabado{tiempoRelativoMillis=" + tiempoRelativoMillis
                + ", idSintetizador=" + idSintetizador
                + ", tipoAccion=" + tipoAccion
                + ", parametros=" + Arrays.toString(parametros) + '}';
    }
}
