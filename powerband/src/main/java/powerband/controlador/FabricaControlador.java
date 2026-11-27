package powerband.controlador;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.LockSupport;

import powerband.comandos.BusComandos;
import powerband.comandos.FabricaComandosGrabados;
import powerband.grabaciones.ArchivosGrabacion;
import powerband.grabaciones.CargadorGrabaciones;
import powerband.grabaciones.FinalizadorGrabacion;
import powerband.grabaciones.Grabador;
import powerband.grabaciones.Reproductor;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.sintetizadores.IdSintetizador;
import powerband.sintetizadores.Sintetizador;

public final class FabricaControlador {

    private FabricaControlador() {
    }

    public static Controlador crear() throws IOException {
        return crear(ArchivosGrabacion.inicializar());
    }

    protected static Controlador crear(ArchivosGrabacion archivos) {
        Objects.requireNonNull(archivos, "archivos");
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        EntradaMicrofono entradaMicrofono = new EntradaMicrofono(microfono);
        EntradaControl entradaControl = new EntradaControl(control);
        EntradaTableta entradaTableta = new EntradaTableta(tableta);
        Map<IdSintetizador, Sintetizador> sintetizadores =
                new ConcurrentHashMap<>();
        Grabador grabador = new Grabador(archivos.directorio());
        CargadorGrabaciones cargador = new CargadorGrabaciones(archivos);
        FinalizadorGrabacion finalizador =
                new FinalizadorGrabacion(grabador, archivos);
        FabricaComandosGrabados fabrica =
                new FabricaComandosGrabados(sintetizadores::get);
        Reproductor reproductor = new Reproductor(
                fabrica::reconstruir, System::nanoTime, LockSupport::parkNanos);
        return new Controlador(
                bus, microfono, control, tableta, entradaMicrofono,
                entradaControl, entradaTableta, sintetizadores, grabador,
                cargador, finalizador, reproductor, archivos);
    }
}
