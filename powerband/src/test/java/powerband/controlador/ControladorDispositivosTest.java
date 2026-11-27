package powerband.controlador;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
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
import powerband.util.Configuraciones;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControladorDispositivosTest {

    @Test
    void iniciarAsignaPianoPredeterminadoAlControl() throws Exception {
        BusComandos bus = new BusComandos();
        Microfono microfono = new Microfono(null, bus);
        Control control = new Control(null, bus);
        Tableta tableta = new Tableta(null, bus);
        ControladorDispositivos controlador = new ControladorDispositivos(
                microfono, control, tableta,
                entradaMicrofonoInactiva(microfono),
                entradaControlInactivo(control),
                entradaTabletaInactiva(tableta),
                new HashMap<>(),
                instrumento -> List.of(),
                (mensaje, causa) -> { },
                (entrada, motivo, causa) -> { });

        controlador.iniciar();

        assertNotNull(control.sintetizador());
        assertNotNull(microfono.sintetizador());
        assertNotNull(tableta.sintetizador());
        assertEquals(List.of(powerband.sintetizadores.IdSintetizador.PIANO),
                controlador.sintetizadoresPorInstrumento().get("control"));
        controlador.liberar(null);
    }

    @Test
    void iniciarEnviaTonoDeVerificacionConTresSintetizadores()
            throws Exception {
        Configuraciones configuraciones = Configuraciones.getInstance();
        String hostAnterior = configuraciones.getHost();
        int puertoAnterior = configuraciones.getPuerto();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");

        try (DatagramSocket receptor = new DatagramSocket(0, loopback)) {
            receptor.setSoTimeout(2000);
            ControladorDispositivos controlador = null;
            try {
                configuraciones.setHost(loopback.getHostAddress());
                configuraciones.setPuerto(receptor.getLocalPort());

                BusComandos bus = new BusComandos();
                Microfono microfono = new Microfono(null, bus);
                Control control = new Control(null, bus);
                Tableta tableta = new Tableta(null, bus);
                controlador = new ControladorDispositivos(
                        microfono,
                        control,
                        tableta,
                        entradaMicrofonoInactiva(microfono),
                        entradaControlInactivo(control),
                        entradaTabletaInactiva(tableta),
                        new HashMap<>(),
                        instrumento -> List.of(),
                        (mensaje, causa) -> { },
                        (entrada, motivo, causa) -> { });

                controlador.iniciar();

                List<String> rutas = new ArrayList<>();
                List<String> sintetizadores = new ArrayList<>();
                for (int indice = 0; indice < 8; indice++) {
                    byte[] datos = new byte[512];
                    DatagramPacket paquete =
                            new DatagramPacket(datos, datos.length);
                    receptor.receive(paquete);
                    String ruta = leerCadenaOsc(paquete, paquete.getOffset());
                    rutas.add(ruta);
                    if ("/s_new".equals(ruta)) {
                        sintetizadores.add(leerNombreSintetizador(paquete));
                    }
                }

                assertEquals(List.of(
                        "/s_new", "/s_new", "/s_new", "/n_set",
                        "/s_new", "/s_new", "/s_new", "/n_set"), rutas);
                assertEquals(List.of(
                        "classicPiano", "stringBass", "organ70s",
                        "classicPiano", "stringBass", "organ70s"),
                        sintetizadores);
            } finally {
                try {
                    if (controlador != null) {
                        Throwable fallo = controlador.liberar(
                                controlador.detenerEntradas(null));
                        assertNull(fallo);
                    }
                } finally {
                    configuraciones.setHost(hostAnterior);
                    configuraciones.setPuerto(puertoAnterior);
                }
            }
        }
    }

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

    private static EntradaMicrofono entradaMicrofonoInactiva(
            Microfono microfono) {
        return new EntradaMicrofono(microfono) {
            @Override
            public void iniciar() {
            }

            @Override
            public void detener() {
            }

            @Override
            public boolean estaActiva() {
                return false;
            }
        };
    }

    private static EntradaControl entradaControlInactivo(Control control) {
        return new EntradaControl(control) {
            @Override
            public void iniciar() {
            }

            @Override
            public void detener() {
            }

            @Override
            public boolean estaActiva() {
                return false;
            }
        };
    }

    private static EntradaTableta entradaTabletaInactiva(Tableta tableta) {
        return new EntradaTableta(tableta) {
            @Override
            public void iniciar() {
            }

            @Override
            public void detener() {
            }

            @Override
            public boolean estaActiva() {
                return false;
            }
        };
    }

    private static String leerNombreSintetizador(
            DatagramPacket paquete) {
        int desplazamiento = paquete.getOffset();
        desplazamiento = desplazamientoDespuesCadena(
                paquete.getData(), desplazamiento);
        desplazamiento = desplazamientoDespuesCadena(
                paquete.getData(), desplazamiento);
        return leerCadenaOsc(paquete, desplazamiento);
    }

    private static int desplazamientoDespuesCadena(
            byte[] datos, int desplazamiento) {
        int inicio = desplazamiento;
        while (datos[desplazamiento] != 0) {
            desplazamiento++;
        }
        int longitudConTerminador = desplazamiento - inicio + 1;
        return inicio + (longitudConTerminador + 3) / 4 * 4;
    }

    private static String leerCadenaOsc(
            DatagramPacket paquete, int desplazamiento) {
        int inicio = desplazamiento;
        while (paquete.getData()[desplazamiento] != 0) {
            desplazamiento++;
        }
        return new String(
                paquete.getData(),
                inicio,
                desplazamiento - inicio,
                StandardCharsets.US_ASCII);
    }
}
