package powerband.grabaciones;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import powerband.archivos.ComandoGrabado;
import powerband.archivos.EncabezadoGrabacion;
import powerband.excepciones.ArchivoException;
import powerband.util.Constantes;

public final class GrabacionAbierta implements AutoCloseable {

    private final FileChannel canal;
    private final Path rutaMusica;
    private final EncabezadoGrabacion encabezado;
    private final long posicionFinal;
    private long siguientePosicion;

    protected GrabacionAbierta(FileChannel canal, Path rutaMusica,
            EncabezadoGrabacion encabezado, long siguientePosicion,
            long posicionFinal) {
        this.canal = Objects.requireNonNull(canal, "canal");
        this.rutaMusica = Objects.requireNonNull(rutaMusica, "rutaMusica");
        this.encabezado = Objects.requireNonNull(encabezado, "encabezado");
        this.siguientePosicion = siguientePosicion;
        this.posicionFinal = posicionFinal;
    }

    public EncabezadoGrabacion encabezado() {
        return encabezado;
    }

    public Optional<ComandoGrabado> siguienteComando()
            throws IOException {
        if (siguientePosicion == posicionFinal) {
            return Optional.empty();
        }

        byte[] datos = leerBytes(
                canal, rutaMusica, siguientePosicion,
                Constantes.TAMANO_COMANDO_GRABADO);
        ComandoGrabado comando;
        try {
            comando = ComandoGrabado.desdeBytes(datos);
        } catch (IllegalArgumentException excepcion) {
            throw ArchivoException.registroCorrupto(
                    rutaMusica.toString(), excepcion);
        }

        siguientePosicion += Constantes.TAMANO_COMANDO_GRABADO;
        return Optional.of(comando);
    }

    @Override
    public void close() throws IOException {
        try {
            canal.close();
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(
                    rutaMusica.toString(),
                    "cerrar la lectura de la grabación", excepcion);
        }
    }

    public static byte[] leerBytes(FileChannel canal, Path ruta,
            long posicion, int cantidad) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(cantidad);
        try {
            while (buffer.hasRemaining()) {
                int leidos = canal.read(
                        buffer, posicion + buffer.position());
                if (leidos < 0) {
                    throw ArchivoException.registroFueraDeRango(
                            ruta.toString(),
                            posicion + buffer.position());
                }
                if (leidos == 0) {
                    throw ArchivoException.errorES(
                            ruta.toString(),
                            "leer registros de una grabación", null);
                }
            }
            return buffer.array();
        } catch (ArchivoException excepcion) {
            throw excepcion;
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(
                    ruta.toString(),
                    "leer registros de una grabación", excepcion);
        }
    }
}
