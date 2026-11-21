package powerband.archivos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.function.Function;

import powerband.excepciones.ArchivoException;
import powerband.excepciones.ArchivoIncompletoException;
import powerband.excepciones.ArgumentoInvalidoException;
import powerband.logging.LoggerPowerBand;

//Permite facilitar las operaciones con archivos y hacerlo de forma flexible
public final class ArchivoRegistros<T> implements AutoCloseable {

    private final Path ruta;
    private final int tamanoRegistro;
    private final Function<byte[], T> decodificador;
    private final Function<T, byte[]> serializador;
    private FileChannel canalLectura;
    private FileChannel canalEscritura;
    private boolean cerrado;

    public ArchivoRegistros(Path ruta, int tamanoRegistro,
            Function<byte[], T> decodificador,
            Function<T, byte[]> serializador) {
        this.ruta = Objects.requireNonNull(ruta, "ruta");
        if (tamanoRegistro <= 0) {
            throw ArgumentoInvalidoException.tamanoRegistroInvalido();
        }
        this.tamanoRegistro = tamanoRegistro;
        this.decodificador = Objects.requireNonNull(
                decodificador, "decodificador");
        this.serializador = Objects.requireNonNull(
                serializador, "serializador");
    }

    public long cantidadRegistros() throws IOException {
        try {
            FileChannel canal = obtenerCanalLectura();
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            return tamanoArchivo / tamanoRegistro;
        } catch (ArchivoException | ArgumentoInvalidoException excepcion) {
            if (excepcion instanceof ArchivoException) {
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        excepcion.getMessage(), ruta.toString());
            }
            throw excepcion;
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(
                    ruta.toString(), "contar registros", excepcion);
        }
    }

    public T leer(long posicionBytes) throws IOException {
        validarPosicion(posicionBytes);
        try {
            FileChannel canal = obtenerCanalLectura();
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            if (posicionBytes > tamanoArchivo - tamanoRegistro) {
                throw ArchivoException.registroFueraDeRango(
                        ruta.toString(), posicionBytes);
            }

            ByteBuffer buffer = ByteBuffer.allocate(tamanoRegistro);
            leerCompletamente(canal, buffer, posicionBytes);
            return decodificar(buffer.array());
        } catch (ArchivoException | ArgumentoInvalidoException excepcion) {
            throw excepcion;
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(ruta.toString(),
                    "leer registro en offset " + posicionBytes, excepcion);
        }
    }

    public void escribir(long posicionBytes, T registro) throws IOException {
        validarPosicion(posicionBytes);
        Objects.requireNonNull(registro, "registro");
        byte[] datos = Objects.requireNonNull(
                serializador.apply(registro), "registro serializado");
        if (datos.length != tamanoRegistro) {
            throw ArgumentoInvalidoException.tamanoRegistroInvalido();
        }

        try {
            FileChannel canal = obtenerCanalEscritura();
            long tamanoArchivo = canal.size();
            validarLongitud(tamanoArchivo);
            if (posicionBytes > tamanoArchivo) {
                throw ArchivoException.escrituraConHueco(
                        ruta.toString(), posicionBytes, tamanoArchivo);
            }
            if (posicionBytes > Long.MAX_VALUE - tamanoRegistro) {
                throw ArchivoException.escrituraConHueco(
                        ruta.toString(), posicionBytes, tamanoArchivo);
            }

            ByteBuffer buffer = ByteBuffer.wrap(datos);
            escribirCompletamente(canal, buffer, posicionBytes);
        } catch (ArchivoException | ArgumentoInvalidoException excepcion) {
            throw excepcion;
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(ruta.toString(),
                    "escribir registro en offset " + posicionBytes,
                    excepcion);
        }
    }

    @Override
    public void close() throws IOException {
        if (cerrado) {
            return;
        }
        cerrado = true;
        IOException fallo = null;
        if (canalLectura != null) {
            try {
                canalLectura.close();
            } catch (IOException excepcion) {
                fallo = excepcion;
            }
        }
        if (canalEscritura != null) {
            try {
                canalEscritura.close();
            } catch (IOException excepcion) {
                if (fallo == null) {
                    fallo = excepcion;
                } else {
                    fallo.addSuppressed(excepcion);
                }
            }
        }
        if (fallo != null) {
            throw ArchivoException.errorES(
                    ruta.toString(), "cerrar el archivo", fallo);
        }
    }

    private FileChannel obtenerCanalLectura() throws IOException {
        validarAbierto();
        if (canalLectura == null) {
            canalLectura = FileChannel.open(ruta, StandardOpenOption.READ);
        }
        return canalLectura;
    }

    private FileChannel obtenerCanalEscritura() throws IOException {
        validarAbierto();
        if (canalEscritura == null) {
            canalEscritura = FileChannel.open(ruta,
                    StandardOpenOption.READ, StandardOpenOption.WRITE);
        }
        return canalEscritura;
    }

    private void validarAbierto() throws IOException {
        if (cerrado) {
            throw ArchivoException.archivoCerrado(ruta.toString());
        }
    }

    private void validarPosicion(long posicionBytes) {
        if (posicionBytes < 0 || posicionBytes % tamanoRegistro != 0) {
            throw ArgumentoInvalidoException.offsetInvalido(
                    tamanoRegistro, posicionBytes);
        }
    }

    private void validarLongitud(long tamanoArchivo) throws IOException {
        long residuo = tamanoArchivo % tamanoRegistro;
        if (residuo != 0) {
            throw new ArchivoIncompletoException(
                    ruta.toString(), tamanoArchivo, tamanoRegistro);
        }
    }

    private T decodificar(byte[] datos) throws IOException {
        try {
            T registro = decodificador.apply(datos);
            if (registro == null) {
                throw ArgumentoInvalidoException.nombreInvalido();
            }
            return registro;
        } catch (ArgumentoInvalidoException excepcion) {
            throw ArchivoException.registroCorrupto(
                    ruta.toString(), excepcion);
        }
    }

    private void leerCompletamente(FileChannel canal, ByteBuffer buffer,
            long posicionBytes) throws IOException {
        while (buffer.hasRemaining()) {
            int leidos = canal.read(buffer, posicionBytes + buffer.position());
            if (leidos < 0) {
                throw ArchivoException.registroFueraDeRango(
                        ruta.toString(), posicionBytes);
            }
            if (leidos == 0) {
                throw ArchivoException.errorES(
                        ruta.toString(), "leer el registro", null);
            }
        }
    }

    private void escribirCompletamente(FileChannel canal, ByteBuffer buffer,
            long posicionBytes) throws IOException {
        while (buffer.hasRemaining()) {
            int escritos = canal.write(buffer, posicionBytes + buffer.position());
            if (escritos == 0) {
                throw ArchivoException.errorES(
                        ruta.toString(), "escribir el registro", null);
            }
        }
    }

}
