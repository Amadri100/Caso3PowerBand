package powerband.instrumentos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import powerband.comandos.BusComandos;
import powerband.comandos.Comando;
import powerband.comandos.ComandoActualizacion;
import powerband.comandos.ComandoFinalizacion;
import powerband.comandos.ComandoInicio;
import powerband.comandos.ComandoPuntual;
import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.Sintetizador;

public abstract class Instrumento {

    private Sintetizador sintetizador;
    private final List<Sintetizador> sintetizadores = new ArrayList<>();
    private final BusComandos busComandos;

    protected Instrumento() {
        this((Sintetizador) null, new BusComandos());
    }

    protected Instrumento(Sintetizador sintetizador) {
        this(sintetizador, new BusComandos());
    }

    protected Instrumento(BusComandos busComandos) {
        this((Sintetizador) null, busComandos);
    }

    protected Instrumento(Sintetizador sintetizador, BusComandos busComandos) {
        this.sintetizador = sintetizador;
        this.busComandos = Objects.requireNonNull(busComandos, "busComandos");
        if (sintetizador != null) {
            sintetizadores.add(sintetizador);
        }
    }

    protected Instrumento(List<? extends Sintetizador> sintetizadores,
                         BusComandos busComandos) {
        this.busComandos = Objects.requireNonNull(busComandos, "busComandos");
        Objects.requireNonNull(sintetizadores, "sintetizadores");
        for (Sintetizador destino : sintetizadores) {
            agregarSintetizador(destino);
        }
        this.sintetizador = this.sintetizadores.isEmpty()
                ? null
                : this.sintetizadores.get(0);
    }

    public Sintetizador sintetizador() {
        return sintetizador;
    }

    public Sintetizador obtenerSintetizador() {
        return sintetizador;
    }

    public Sintetizador reemplazarSintetizador(Sintetizador nuevoSintetizador) {
        Sintetizador anterior = sintetizador;
        sintetizador = nuevoSintetizador;
        if (nuevoSintetizador != null) {
            int indice = sintetizadores.indexOf(anterior);
            if (indice >= 0) {
                sintetizadores.set(indice, nuevoSintetizador);
            } else if (!sintetizadores.contains(nuevoSintetizador)) {
                sintetizadores.add(nuevoSintetizador);
            }
        } else if (anterior != null) {
            sintetizadores.remove(anterior);
        }
        return anterior;
    }

    public void asignarSintetizador(Sintetizador nuevoSintetizador) {
        reemplazarSintetizador(nuevoSintetizador);
    }

    public void cambiarSintetizador(Sintetizador nuevoSintetizador) {
        reemplazarSintetizador(nuevoSintetizador);
    }

    public void agregarSintetizador(Sintetizador nuevoSintetizador) {
        Objects.requireNonNull(nuevoSintetizador, "nuevoSintetizador");
        boolean existe = sintetizadores.stream()
                .anyMatch(destino -> destino == nuevoSintetizador);
        if (!existe) {
            sintetizadores.add(nuevoSintetizador);
        }
        if (sintetizador == null) {
            sintetizador = nuevoSintetizador;
        }
    }

    public void retirarSintetizador(Sintetizador sintetizadorRetirar) {
        Objects.requireNonNull(sintetizadorRetirar, "sintetizadorRetirar");
        sintetizadores.removeIf(destino -> destino == sintetizadorRetirar);
        if (sintetizador == sintetizadorRetirar) {
            sintetizador = sintetizadores.isEmpty()
                    ? null
                    : sintetizadores.get(0);
        }
    }

    public List<Sintetizador> sintetizadores() {
        return List.copyOf(sintetizadores);
    }

    public BusComandos busComandos() {
        return busComandos;
    }

    public void enviarAccion(TipoAccion tipoAccion, double... parametros) {
        if (sintetizadores.isEmpty()) {
            throw new IllegalStateException(
                    "El instrumento no tiene un sintetizador asignado");
        }
        for (Sintetizador destino : List.copyOf(sintetizadores)) {
            Comando comando;
            if (tipoAccion == TipoAccion.PUNTUAL) {
                comando = new ComandoPuntual(destino, parametros);
            } else if (tipoAccion == TipoAccion.INICIO) {
                comando = new ComandoInicio(destino, parametros);
            } else if (tipoAccion == TipoAccion.ACTUALIZACION) {
                comando = new ComandoActualizacion(destino, parametros);
            } else if (tipoAccion == TipoAccion.FINALIZACION) {
                comando = new ComandoFinalizacion(destino, parametros);
            } else {
                throw new IllegalArgumentException(
                        "Tipo de accion desconocido");
            }
            enviarComando(comando);
        }
    }

    public void ejecutarAccion(TipoAccion tipoAccion, double... parametros) {
        enviarAccion(tipoAccion, parametros);
    }

    public void enviarComando(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        comando.ejecutar();
        busComandos.publicar(comando, System.nanoTime());
    }

    protected void publicarComando(Comando comando) {
        enviarComando(comando);
    }

    protected void registrarError(String mensaje, RuntimeException excepcion) {
        LoggerPowerBand.getInstance().registrarError(
                mensaje + ": " + excepcion);
    }
}
