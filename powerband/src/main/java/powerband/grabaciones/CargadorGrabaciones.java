package powerband.grabaciones;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import powerband.archivos.BuscadorIndice;
import powerband.archivos.EncabezadoGrabacion;
import powerband.archivos.EntradaIndice;
import powerband.archivos.EntradaListaGrabacion;
import powerband.archivos.ListaGrabaciones;
import powerband.archivos.ValidacionesGenerales;
import powerband.excepciones.ArchivoException;
import powerband.excepciones.ArchivoIncompletoException;
import powerband.util.Constantes;

public final class CargadorGrabaciones {

    private final ArchivosGrabacion archivos;

    public CargadorGrabaciones(ArchivosGrabacion archivos) {
        this.archivos = Objects.requireNonNull(archivos, "archivos");
    }
    //Lista de entradas
    public List<EntradaListaGrabacion> listar() throws IOException {
        return new ListaGrabaciones(archivos.archivoLista()).listar();
    }
    //Metodo de acceso a abrir un string desde afuera
    public Optional<GrabacionAbierta> abrir(String nombre)
            throws IOException {
        ValidacionesGenerales.validarNombre(nombre);
        //Llama al buscador que ya tiene el algoritmo
        Optional<EntradaIndice> entrada = new BuscadorIndice(
                archivos.archivoIndice()).buscar(nombre);
        if (entrada.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(abrir(entrada.get()));
    }
    //Abrir interno, recibe la entrada y devuelve la grabacion
    private GrabacionAbierta abrir(EntradaIndice entrada)
            throws IOException {
        Path rutaMusica = archivos.archivoMusica();
        FileChannel canal;
        //Abre el canal
        try {
            canal = FileChannel.open(rutaMusica, StandardOpenOption.READ);
        } catch (IOException excepcion) {
            throw ArchivoException.errorES(
                    rutaMusica.toString(),
                    "abrir una grabación", excepcion);
        }
        try {
            long posicionInicial = entrada.posicionInicial();
            long posicionFinal = entrada.posicionFinal();
            long tamanoMusica;
            //Validacion de intervalos
            try {
                tamanoMusica = canal.size();
            } catch (IOException excepcion) {
                throw ArchivoException.errorES(
                        rutaMusica.toString(),
                        "obtener el tamaño de la música", excepcion);
            }
            validarIntervalo(rutaMusica, posicionInicial,
                    posicionFinal, tamanoMusica);
            //Lectura del encabezado
            byte[] datosEncabezado = GrabacionAbierta.leerBytes(
                    canal, rutaMusica, posicionInicial,
                    Constantes.TAMANO_ENCABEZADO_GRABACION);
            EncabezadoGrabacion encabezado;
           
            try {
                encabezado =
                        EncabezadoGrabacion.desdeBytes(datosEncabezado);
            } catch (IllegalArgumentException excepcion) {
                throw ArchivoException.registroCorrupto(
                        rutaMusica.toString(), excepcion);
            }
            //Revisa si el encabezado es correcto
            if (!encabezado.nombre().equals(entrada.nombre())
                    || encabezado.posicionInicial() != posicionInicial
                    || encabezado.posicionFinal() != posicionFinal) {
                throw ArchivoException.registroCorrupto(
                        rutaMusica.toString(),
                        new IllegalArgumentException(
                                "El encabezado no coincide con el índice"));
            }
            //Retorna la grabacion si paso todas las verficiaciones
            long posicionPrimerComando = posicionInicial
                    + Constantes.TAMANO_ENCABEZADO_GRABACION;
            return new GrabacionAbierta(
                    canal, rutaMusica, encabezado,
                    posicionPrimerComando, posicionFinal);
        } catch (IOException | RuntimeException | Error fallo) {
            try {
                canal.close();
            } catch (IOException falloCierre) {
                fallo.addSuppressed(falloCierre);
            }
            throw fallo;
        }
    }
    //Valida que el intervalo sea valido
    //Valida que el archivo tenga un tamaño apropiado y las posiciones sean posibles.
    private void validarIntervalo(Path rutaMusica,
            long posicionInicial, long posicionFinal, long tamanoMusica)
            throws IOException {
        // Los intervalos tiene sentido
        if (posicionInicial < 0 || posicionFinal < posicionInicial) {
            throw ArchivoException.registroCorrupto(
                    rutaMusica.toString(),
                    new IllegalArgumentException(
                            "El intervalo del índice no es válido"));
        }
        //No se sale del archivo
        if (posicionFinal > tamanoMusica) {
            throw ArchivoException.registroFueraDeRango(
                    rutaMusica.toString(), posicionFinal);
        }
        //El tamaño de los intervalos es suficiente para al menos 
        //tener el header
        long tamanoIntervalo = posicionFinal - posicionInicial;
        if (tamanoIntervalo
                < Constantes.TAMANO_ENCABEZADO_GRABACION) {
            throw new ArchivoIncompletoException(
                    rutaMusica.toString(), tamanoIntervalo,
                    Constantes.TAMANO_ENCABEZADO_GRABACION);
        }
        //El tamaño sin el header se puede dividir en comandos.
        long tamanoComandos = tamanoIntervalo
                - Constantes.TAMANO_ENCABEZADO_GRABACION;
        if (tamanoComandos % Constantes.TAMANO_COMANDO_GRABADO != 0) {
            throw new ArchivoIncompletoException(
                    rutaMusica.toString(), tamanoComandos,
                    Constantes.TAMANO_COMANDO_GRABADO);
        }
    }
}
