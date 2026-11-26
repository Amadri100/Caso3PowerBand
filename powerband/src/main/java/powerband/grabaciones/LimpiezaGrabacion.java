package powerband.grabaciones;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import powerband.excepciones.ArchivoException;
import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;

public final class LimpiezaGrabacion {

    private final Path archivoTemporal;
    private final ArchivosGrabacion archivos;

    public LimpiezaGrabacion(Path archivoTemporal,
            ArchivosGrabacion archivos) {
        this.archivos = Objects.requireNonNull(
                archivos, "archivos");
        Path rutaTemporal = Objects.requireNonNull(
                archivoTemporal, "archivoTemporal")
                .toAbsolutePath().normalize();
        if (rutaTemporal.equals(archivos.archivoMusica())
                || rutaTemporal.equals(archivos.archivoLista())
                || rutaTemporal.equals(archivos.archivoIndice())) {
            throw new IllegalArgumentException(
                    "La ruta no es un temporal de grabación válido");
        }
        this.archivoTemporal = rutaTemporal;
    }

    public void limpiar(Throwable fallo) {
        Objects.requireNonNull(fallo, "fallo");
        try {
            eliminarTemporal();
        } catch (IOException | RuntimeException excepcionLimpieza) {
            fallo.addSuppressed(excepcionLimpieza);
        }

        try {
            String mensaje = fallo.getMessage();
            if (mensaje == null || mensaje.isBlank()) {
                mensaje = fallo.getClass().getSimpleName();
            }
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    mensaje, archivos.directorio().toString(), OrigenLog.GRABACIONES, fallo));
        } catch (RuntimeException falloRegistro) {
            fallo.addSuppressed(falloRegistro);
        }
    }

    public void limpiarExitosamente() throws IOException {
        try {
            eliminarTemporal();
        } catch (IOException | RuntimeException excepcionLimpieza) {
            throw ArchivoException.errorES(
                    archivoTemporal.toString(),
                    "eliminar el temporal de grabación",
                    excepcionLimpieza);
        }
    }

    private void eliminarTemporal() throws IOException {
        if (Files.isDirectory(archivoTemporal)) {
            throw ArchivoException.rutaNoRegular(
                    archivoTemporal.toString());
        }
        Files.deleteIfExists(archivoTemporal);
    }
}
