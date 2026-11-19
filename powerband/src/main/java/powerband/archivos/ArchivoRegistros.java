package powerband.archivos;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Function;

import powerband.logging.LoggerPowerBand;

//Permite facilitar las operaciones con archivos y hacerlo de forma flexible
public final class ArchivoRegistros<T> {

    private final Path ruta;
    private final int tamanoRegistro;
    private final Function<byte[], T> decodificador;
    private final Function<T, byte[]> serializador;

    public ArchivoRegistros(Path ruta, int tamanoRegistro,
            Function<byte[], T> decodificador,
            Function<T, byte[]> serializador) {
        this.ruta = Objects.requireNonNull(ruta, "ruta");
        if (tamanoRegistro <= 0) {
            throw new IllegalArgumentException(
                    "El tamaño del registro debe ser positivo");
        }
        this.tamanoRegistro = tamanoRegistro;
        this.decodificador = Objects.requireNonNull(
                decodificador, "decodificador");
        this.serializador = Objects.requireNonNull(
                serializador, "serializador");
    }

    public long cantidadRegistros() throws IOException {
        try (FileChannel canal = FileChannel.open(ruta,
                StandardOpenOption.READ)) {
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            return tamanoArchivo / tamanoRegistro;
        } catch (IOException excepcion) {
            throw informarFallo("contar registros", excepcion);
        }
    }

    public T leer(long posicionBytes) throws IOException {
        validarPosicion(posicionBytes);
        try (FileChannel canal = FileChannel.open(ruta,
                StandardOpenOption.READ)) {
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            if (posicionBytes > tamanoArchivo - tamanoRegistro) {
                throw new EOFException(
                        "No hay un registro completo en el offset "
                                + posicionBytes);
            }

            ByteBuffer buffer = ByteBuffer.allocate(tamanoRegistro);
            leerCompletamente(canal, buffer, posicionBytes);
            return decodificar(buffer.array());
        } catch (IOException excepcion) {
            throw informarFallo("leer registro en offset " + posicionBytes,
                    excepcion);
        }
    }

    public void escribir(long posicionBytes, T registro) throws IOException {
        validarPosicion(posicionBytes);
        Objects.requireNonNull(registro, "registro");
        byte[] datos = Objects.requireNonNull(
                serializador.apply(registro), "registro serializado");
        if (datos.length != tamanoRegistro) {
            throw new IllegalArgumentException(
                    "El registro serializado debe tener "
                            + tamanoRegistro + " bytes; se recibieron "
                            + datos.length);
        }

        try (FileChannel canal = FileChannel.open(ruta,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            if (posicionBytes > tamanoArchivo) {
                throw new IOException(
                        "El offset " + posicionBytes
                                + " crearia un espacio despues del final "
                                + "del archivo (" + tamanoArchivo + " bytes)");
            }
            if (posicionBytes > Long.MAX_VALUE - tamanoRegistro) {
                throw new IOException(
                        "El offset excede el limite de posiciones del archivo");
            }

            ByteBuffer buffer = ByteBuffer.wrap(datos);
            escribirCompletamente(canal, buffer, posicionBytes);
        } catch (IOException excepcion) {
            throw informarFallo("escribir registro en offset " + posicionBytes,
                    excepcion);
        }
    }

    private void validarPosicion(long posicionBytes) {
        if (posicionBytes < 0 || posicionBytes % tamanoRegistro != 0) {
            throw new IllegalArgumentException(
                    "El offset debe ser no negativo y multiplo de "
                            + tamanoRegistro + ": " + posicionBytes);
        }
    }

    private void validarLongitud(long tamanoArchivo) throws IOException {
        long residuo = tamanoArchivo % tamanoRegistro;
        if (residuo != 0) {
            throw new IOException(
                    "Archivo incompleto: tamaño " + tamanoArchivo
                            + " bytes, registro de " + tamanoRegistro
                            + " bytes, residuo " + residuo);
        }
    }

    private T decodificar(byte[] datos) throws IOException {
        try {
            T registro = decodificador.apply(datos);
            if (registro == null) {
                throw new IllegalArgumentException(
                        "El decodificador devolvio un registro nulo");
            }
            return registro;
        } catch (IllegalArgumentException excepcion) {
            throw new IOException("El registro no cumple su formato", excepcion);
        }
    }

    private void leerCompletamente(FileChannel canal, ByteBuffer buffer,
            long posicionBytes) throws IOException {
        while (buffer.hasRemaining()) {
            int leidos = canal.read(buffer, posicionBytes + buffer.position());
            if (leidos < 0) {
                throw new EOFException("Fin inesperado dentro del registro");
            }
            if (leidos == 0) {
                throw new IOException("La lectura no avanzo");
            }
        }
    }

    private void escribirCompletamente(FileChannel canal, ByteBuffer buffer,
            long posicionBytes) throws IOException {
        while (buffer.hasRemaining()) {
            int escritos = canal.write(buffer, posicionBytes + buffer.position());
            if (escritos == 0) {
                throw new IOException("La escritura no avanzo");
            }
        }
    }

    private IOException informarFallo(String operacion,
            IOException excepcion) {
        String mensaje = "No se pudo " + operacion + " en " + ruta
                + ": " + excepcion.getMessage();
        LoggerPowerBand.getInstance().registrarErrorArchivo(
                mensaje, ruta.toString());
        return new IOException(mensaje, excepcion);
    }
}
