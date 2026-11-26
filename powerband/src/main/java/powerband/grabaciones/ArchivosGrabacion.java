package powerband.grabaciones;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import powerband.logging.LoggerPowerBand;
import powerband.logging.Log;
import powerband.logging.OrigenLog;
import powerband.util.Constantes;
import powerband.excepciones.ArchivoException;

public final class ArchivosGrabacion {

    private final Path directorio;
    private final Path archivoMusica;
    private final Path archivoLista;
    private final Path archivoIndice;

    private ArchivosGrabacion(Path directorio, Path archivoMusica,
            Path archivoLista, Path archivoIndice) {
        this.directorio = directorio;
        this.archivoMusica = archivoMusica;
        this.archivoLista = archivoLista;
        this.archivoIndice = archivoIndice;
    }

    public static ArchivosGrabacion inicializar() throws IOException {
        return inicializar(Path.of(Constantes.RUTA_GRABACIONES));
    }
    //Se encarga de crear ese archivo si fuera necesario
    public static ArchivosGrabacion inicializar(Path directorio)
            throws IOException {
        Path rutaDirectorio = Objects.requireNonNull(
                directorio, "directorio").toAbsolutePath().normalize();
        Path archivoMusica = rutaDirectorio.resolve(
                Constantes.NOMBRE_ARCHIVO_GRABACIONES_MUSICA);
        Path archivoLista = rutaDirectorio.resolve(
                Constantes.NOMBRE_ARCHIVO_GRABACIONES_LISTA);
        Path archivoIndice = rutaDirectorio.resolve(
                Constantes.NOMBRE_ARCHIVO_GRABACIONES_INDICE);

        try {
            Files.createDirectories(rutaDirectorio);
            crearSiFalta(archivoMusica);
            crearSiFalta(archivoLista);
            crearSiFalta(archivoIndice);
        } catch (IOException excepcion) {
            ArchivoException fallo = ArchivoException.errorES(
                    rutaDirectorio.toString(),
                    "preparar los archivos de grabación", excepcion);
            LoggerPowerBand.getInstance().registrar(Log.errorArchivo(
                    fallo.getMessage(), rutaDirectorio.toString(), OrigenLog.GRABACIONES, fallo));
            throw fallo;
        }

        return new ArchivosGrabacion(rutaDirectorio, archivoMusica,
                archivoLista, archivoIndice);
    }

    public Path directorio() {
        return directorio;
    }

    public Path archivoMusica() {
        return archivoMusica;
    }

    public Path archivoLista() {
        return archivoLista;
    }

    public Path archivoIndice() {
        return archivoIndice;
    }

    private static void crearSiFalta(Path ruta) throws IOException {
        try {
            Files.createFile(ruta);
            LoggerPowerBand.getInstance().registrar(Log.info(
                    "Archivo de grabacion creado: " + ruta, OrigenLog.GRABACIONES));
        } catch (FileAlreadyExistsException excepcion) {
            if (!Files.isRegularFile(ruta)) {
                throw ArchivoException.rutaNoRegular(ruta.toString());
            }
        }
    }
}
