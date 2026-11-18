package powerband.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;

import powerband.util.Constantes;

public final class LoggerPowerBand {

    private static final LoggerPowerBand INSTANCIA =
            new LoggerPowerBand(Paths.get(Constantes.RUTA_LOGS));

    private final Path carpetaLogs;

    public LoggerPowerBand(Path carpetaLogs) {
        this.carpetaLogs = Objects.requireNonNull(carpetaLogs, "carpetaLogs");
    }

    public static LoggerPowerBand getInstance() {
        return INSTANCIA;
    }

    public void registrarComando(Object objeto) {
        registrar(CategoriaLog.COMANDO, objeto, null);
    }

    public void registrarInfo(Object objeto) {
        registrar(CategoriaLog.INFO, objeto, null);
    }

    public void registrarError(Object objeto) {
        registrar(CategoriaLog.ERROR, objeto, null);
    }

    public void registrarErrorArchivo(Object objeto, String ubicacion) {
        registrar(CategoriaLog.ERROR, objeto, ubicacion);
    }

    public void registrar(CategoriaLog categoria, Object objeto) {
        registrar(categoria, objeto, null);
    }

    private void registrar(CategoriaLog categoria, Object objeto, String ubicacion) {
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(objeto, "objeto");

        String mensaje = objeto.toString();
        if (mensaje.isBlank()) {
            throw new IllegalArgumentException("El evento no puede estar vacio");
        }

        StringBuilder linea = new StringBuilder();
        linea.append(Instant.now())
                .append(" [")
                .append(categoria.texto())
                .append("] ")
                .append(mensaje.replace('\n', ' ').replace('\r', ' ').strip());

        if (ubicacion != null && !ubicacion.isBlank()) {
            linea.append(" ubicacion=").append(ubicacion);
        }
        linea.append(System.lineSeparator());

        try {
            Files.createDirectories(carpetaLogs);
            Path archivo = carpetaLogs.resolve("powerband.log");
            Files.writeString(archivo, linea.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException excepcion) {
            throw new IllegalStateException(
                    "No se pudo escribir el log en " + carpetaLogs, excepcion);
        }
    }
}
