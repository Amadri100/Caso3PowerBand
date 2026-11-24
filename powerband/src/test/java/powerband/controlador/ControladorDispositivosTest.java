package powerband.controlador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import powerband.comandos.BusComandos;
import powerband.input.EntradaControl;
import powerband.input.EntradaMicrofono;
import powerband.input.EntradaTableta;
import powerband.instrumentos.Control;
import powerband.instrumentos.Microfono;
import powerband.instrumentos.Tableta;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorDispositivosTest {

    @Test
    void validaPorInstrumentoReconsultaYAplicaLaSeleccion() {
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        List<String> aplicadas = new ArrayList<>();
        EntradaMicrofono entradaMicrofono = new EntradaMicrofono(microfono) {
            @Override
            public void seleccionarDispositivo(String id) {
                super.seleccionarDispositivo(id);
                aplicadas.add("microfono:" + id);
            }
        };
        EntradaControl entradaControl = new EntradaControl(control) {
            @Override
            public void seleccionarDispositivo(String id) {
                super.seleccionarDispositivo(id);
                aplicadas.add("control:" + id);
            }
        };
        EntradaTableta entradaTableta = new EntradaTableta(tableta) {
            @Override
            public void seleccionarDispositivo(String id) {
                super.seleccionarDispositivo(id);
                aplicadas.add("tableta:" + id);
            }
        };
        Map<String, List<DispositivoEntrada>> catalogo = new HashMap<>();
        catalogo.put("microfono", List.of(new DispositivoEntrada("audio", "Audio")));
        catalogo.put("control", List.of(new DispositivoEntrada("pad", "Control")));
        catalogo.put("tableta", List.of(new DispositivoEntrada("pen", "Lápiz")));
        List<String> errores = new ArrayList<>();
        ControladorDispositivos controlador = new ControladorDispositivos(
                microfono, control, tableta, entradaMicrofono, entradaControl, entradaTableta,
                new HashMap<>(), catalogo::get, (mensaje, causa) -> errores.add(mensaje),
                (entrada, motivo, causa) -> { });

        assertEquals(List.of(new DispositivoEntrada("pad", "Control")),
                controlador.listarDispositivos("control"));
        assertFalse(controlador.asignarDispositivo(null, "pad"));
        assertFalse(controlador.asignarDispositivo("control", "pen"));
        assertTrue(controlador.asignarDispositivo("control", "pad"));
        assertTrue(controlador.asignarDispositivo("microfono", "audio"));
        assertTrue(controlador.asignarDispositivo("tableta", "pen"));
        assertEquals(List.of("control:pad", "microfono:audio", "tableta:pen"), aplicadas);

        catalogo.put("control", List.of());
        assertFalse(controlador.asignarDispositivo("control", "pad"));
        assertEquals("pad", controlador.dispositivoPorInstrumento().get("control"));
        catalogo.put("microfono", List.of(new DispositivoEntrada("pad", "Duplicado")));
        assertFalse(controlador.asignarDispositivo("microfono", "pad"));
        assertEquals("audio", controlador.dispositivoPorInstrumento().get("microfono"));
        assertEquals(4, errores.size());
    }

    @Test
    void comunicaFallosDeEnumeracionYRechazaInstrumentosDesconocidos() {
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        List<Throwable> errores = new ArrayList<>();
        ControladorDispositivos controlador = new ControladorDispositivos(
                microfono, control, tableta, new EntradaMicrofono(microfono),
                new EntradaControl(control), new EntradaTableta(tableta), new HashMap<>(),
                instrumento -> { throw new IllegalStateException("fallo simulado"); },
                (mensaje, causa) -> errores.add(causa), (entrada, motivo, causa) -> { });

        assertThrows(IllegalArgumentException.class, () -> controlador.listarDispositivos(null));
        assertThrows(IllegalStateException.class, () -> controlador.listarDispositivos("control"));
        assertFalse(controlador.asignarDispositivo("control", "pad"));
        assertEquals(3, errores.size());
    }
}
