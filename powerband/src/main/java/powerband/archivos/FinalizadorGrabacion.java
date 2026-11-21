package powerband.archivos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

import powerband.excepciones.ArchivoException;
import powerband.excepciones.ArchivoIncompletoException;
import powerband.excepciones.NombreDuplicadoException;
import powerband.logging.LoggerPowerBand;
import powerband.util.Constantes;

public final class FinalizadorGrabacion {

    private final Grabador grabador;
    private final ArchivosGrabacion archivos;
    private final LongSupplier relojFecha;

    public FinalizadorGrabacion(Grabador grabador,
            ArchivosGrabacion archivos) {
        this(grabador, archivos, () -> Instant.now().toEpochMilli());
    }

    public FinalizadorGrabacion(Grabador grabador,
            ArchivosGrabacion archivos, LongSupplier relojFecha) {
        this.grabador = Objects.requireNonNull(
                grabador, "grabador");
        this.archivos = Objects.requireNonNull(
                archivos, "archivos");
        this.relojFecha = Objects.requireNonNull(
                relojFecha, "relojFecha");
    }

    public void finalizar(String nombre) throws IOException {
        Path temporalGrabacion = grabador.archivoSesionTemporal();
        LimpiezaGrabacion limpieza =
                new LimpiezaGrabacion(temporalGrabacion, archivos);

        try {
            grabador.detener();
            ValidacionesGenerales.validarNombre(nombre);
            validarTemporal(temporalGrabacion);

            Optional<EntradaIndice> existente =
                    new BuscadorIndice(
                            archivos.archivoIndice()).buscar(nombre);
            if (existente.isPresent()) {
                throw new NombreDuplicadoException(
                        existente.get().nombre(), nombre,
                        archivos.archivoIndice().toString());
            }

            long posicionInicial =
                    Files.size(archivos.archivoMusica());
            long cantidadComandos = grabador.cantidadComandos();
            long posicionFinal = posicionInicial
                    + Constantes.TAMANO_ENCABEZADO_GRABACION
                    + cantidadComandos
                            * Constantes.TAMANO_COMANDO_GRABADO;

            long fecha = relojFecha.getAsLong();
            EncabezadoGrabacion encabezado =
                    new EncabezadoGrabacion(
                            nombre, posicionInicial,
                            posicionFinal, fecha);

            Path musicaTemporal = crearCopia(
                    archivos.archivoMusica(), "musica-final-");
            Path listaTemporal = null;
            Path indiceTemporal = null;
            try {
                listaTemporal = crearCopia(
                        archivos.archivoLista(), "lista-final-");
                indiceTemporal = crearCopia(
                        archivos.archivoIndice(), "indice-final-");
                agregarGrabacion(
                        musicaTemporal, temporalGrabacion,
                        encabezado, posicionInicial);
                new ListaGrabaciones(listaTemporal).agregar(
                        new EntradaListaGrabacion(nombre, fecha));

                agregarIndice(indiceTemporal, nombre,
                        posicionInicial, posicionFinal);

                Path indiceOrdenado =
                        new OrdenadorIndice(indiceTemporal).ordenar();
                try {
                    reemplazarTodos(
                            musicaTemporal, listaTemporal, indiceOrdenado,
                            limpieza);
                } finally {
                    eliminarTemporalesConRegistro(indiceOrdenado);
                }
                eliminarTemporalesConRegistro(
                        musicaTemporal, listaTemporal, indiceTemporal);
            } catch (IOException | RuntimeException | Error fallo) {
                eliminarTemporales(fallo, musicaTemporal,
                        listaTemporal, indiceTemporal);
                throw fallo;
            }
        } catch (IOException | RuntimeException | Error fallo) {
            limpieza.limpiar(fallo);
            throw fallo;
        }
    }

    private void validarTemporal(Path temporal) throws IOException {
        long tamano = Files.size(temporal);
        long residuo = tamano % Constantes.TAMANO_COMANDO_GRABADO;
        if (residuo != 0) {
            throw new ArchivoIncompletoException(
                    temporal.toString(), tamano,
                    Constantes.TAMANO_COMANDO_GRABADO);
        }

        try (ArchivoRegistros<ComandoGrabado> archivo =
                crearArchivoComandos(temporal)) {
            long cantidad = archivo.cantidadRegistros();
            for (long indice = 0; indice < cantidad; indice++) {
                archivo.leer(indice
                        * Constantes.TAMANO_COMANDO_GRABADO);
            }
        }
    }

    private void agregarGrabacion(Path musicaTemporal,
            Path temporalGrabacion,
            EncabezadoGrabacion encabezado,
            long posicionInicial) throws IOException {
        try (FileChannel musica = FileChannel.open(
                    musicaTemporal,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE);
                ArchivoRegistros<ComandoGrabado> comandos =
                    crearArchivoComandos(temporalGrabacion)) {
            long posicion = posicionInicial;
            ByteBuffer encabezadoBytes =
                    ByteBuffer.wrap(encabezado.serializar());
            escribirCompletamente(musica, encabezadoBytes, posicion);
            posicion += Constantes.TAMANO_ENCABEZADO_GRABACION;

            long cantidad = comandos.cantidadRegistros();
            for (long indice = 0; indice < cantidad; indice++) {
                ComandoGrabado comando = comandos.leer(
                        indice * Constantes.TAMANO_COMANDO_GRABADO);
                ByteBuffer datos =
                        ByteBuffer.wrap(comando.serializar());
                escribirCompletamente(musica, datos, posicion);
                posicion += Constantes.TAMANO_COMANDO_GRABADO;
            }
        }
    }

    private void agregarIndice(Path indiceTemporal, String nombre,
            long posicionInicial, long posicionFinal)
            throws IOException {
        try (ArchivoRegistros<EntradaIndice> indice =
                crearArchivoIndice(indiceTemporal)) {
            long cantidad = indice.cantidadRegistros();
            indice.escribir(
                    cantidad * Constantes.TAMANO_ENTRADA_INDICE,
                    new EntradaIndice(
                            nombre, posicionInicial, posicionFinal));
        }
    }

    private void reemplazarTodos(Path musicaTemporal,
            Path listaTemporal, Path indiceOrdenado,
            LimpiezaGrabacion limpieza)
            throws IOException {
        Path musicaBackup = null;
        Path listaBackup = null;
        Path indiceBackup = null;
        boolean musicaReemplazada = false;
        boolean listaReemplazada = false;
        boolean indiceReemplazado = false;
        boolean conservarMusicaBackup = false;
        boolean conservarListaBackup = false;
        boolean conservarIndiceBackup = false;
        Throwable falloPrincipal = null;

        try {
            musicaBackup = crearCopia(
                    archivos.archivoMusica(), "musica-backup-");
            listaBackup = crearCopia(
                    archivos.archivoLista(), "lista-backup-");
            indiceBackup = crearCopia(
                    archivos.archivoIndice(), "indice-backup-");

            reemplazoAtomico(musicaTemporal, archivos.archivoMusica());
            musicaReemplazada = true;
            reemplazoAtomico(listaTemporal, archivos.archivoLista());
            listaReemplazada = true;

            long cantidadIndice;
            try (ArchivoRegistros<EntradaIndice> original =
                    crearArchivoIndice(archivos.archivoIndice())) {
                cantidadIndice = original.cantidadRegistros();
            }

            new ReemplazadorIndice(
                    archivos.archivoIndice(), indiceOrdenado)
                    .reemplazar(cantidadIndice + 1);
            indiceReemplazado = true;
            limpieza.limpiarExitosamente();
        } catch (IOException | RuntimeException | Error fallo) {
            falloPrincipal = fallo;
            if (indiceReemplazado) {
                conservarIndiceBackup = !restaurar(
                        indiceBackup, archivos.archivoIndice(), fallo);
            }
            if (musicaReemplazada) {
                conservarMusicaBackup = !restaurar(
                        musicaBackup, archivos.archivoMusica(), fallo);
            }
            if (listaReemplazada) {
                conservarListaBackup = !restaurar(
                        listaBackup, archivos.archivoLista(), fallo);
            }
            throw fallo;
        } finally {
            if (falloPrincipal == null) {
                eliminarTemporalesConRegistro(
                        musicaBackup, listaBackup, indiceBackup);
            } else {
                eliminarTemporales(falloPrincipal,
                        conservarMusicaBackup ? null : musicaBackup,
                        conservarListaBackup ? null : listaBackup,
                        conservarIndiceBackup ? null : indiceBackup);
            }
        }
    }

    private Path crearCopia(Path origen, String prefijo)
            throws IOException {
        Path destino = Files.createTempFile(
                origen.getParent(), prefijo, ".tmp");
        try {
            Files.copy(origen, destino,
                    StandardCopyOption.REPLACE_EXISTING);
            return destino;
        } catch (IOException | RuntimeException | Error fallo) {
            eliminarTemporales(fallo, destino);
            throw fallo;
        }
    }

    private void reemplazoAtomico(Path origen, Path destino)
            throws IOException {
        try {
            Files.move(origen, destino,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException excepcion) {
            throw new ArchivoException(
                    powerband.excepciones.tipos.TipoErrorArchivo
                            .REEMPLAZO_NO_ATOMICO,
                    destino.toString(), excepcion);
        }
    }

    private boolean restaurar(Path respaldo, Path destino,
            Throwable fallo) {
        try {
            reemplazoAtomico(respaldo, destino);
            return true;
        } catch (IOException | RuntimeException excepcion) {
            fallo.addSuppressed(excepcion);
            return false;
        }
    }

    private void eliminarTemporalesConRegistro(Path... temporales) {
        for (Path temporal : temporales) {
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException | RuntimeException excepcion) {
                ArchivoException fallo = ArchivoException.errorES(
                        temporal.toString(),
                        "eliminar un respaldo temporal",
                        excepcion);
                LoggerPowerBand.getInstance().registrarErrorArchivo(
                        fallo.getMessage(),
                        temporal.toString());
            }
        }
    }

    private void eliminarTemporales(Throwable fallo,
            Path... temporales) {
        for (Path temporal : temporales) {
            if (temporal == null) {
                continue;
            }
            try {
                Files.deleteIfExists(temporal);
            } catch (IOException | RuntimeException excepcion) {
                fallo.addSuppressed(excepcion);
            }
        }
    }

    private void escribirCompletamente(FileChannel canal,
            ByteBuffer buffer, long posicion) throws IOException {
        while (buffer.hasRemaining()) {
            canal.write(buffer, posicion + buffer.position());
        }
    }

    private static ArchivoRegistros<ComandoGrabado>
            crearArchivoComandos(Path ruta) {
        return new ArchivoRegistros<>(
                ruta,
                Constantes.TAMANO_COMANDO_GRABADO,
                ComandoGrabado::desdeBytes,
                ComandoGrabado::serializar);
    }

    private static ArchivoRegistros<EntradaIndice>
            crearArchivoIndice(Path ruta) {
        return new ArchivoRegistros<>(
                ruta,
                Constantes.TAMANO_ENTRADA_INDICE,
                EntradaIndice::desdeBytes,
                EntradaIndice::serializar);
    }
}
