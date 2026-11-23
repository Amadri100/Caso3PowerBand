package powerband.sintetizadores;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import powerband.util.Configuraciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BandClientTest {

    private final Configuraciones configuraciones =
            Configuraciones.getInstance();
    private String hostAnterior;
    private int puertoAnterior;

    @AfterEach
    void restaurarConfiguraciones() {
        if (hostAnterior != null) {
            configuraciones.setHost(hostAnterior);
            configuraciones.setPuerto(puertoAnterior);
        }
    }

    @Test
    void constructorPredeterminadoUsaConfiguracionesCompartidas()
            throws Exception {
        hostAnterior = configuraciones.getHost();
        puertoAnterior = configuraciones.getPuerto();

        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        try (DatagramSocket receptor = new DatagramSocket(0, loopback)) {
            receptor.setSoTimeout(2000);
            configuraciones.setHost(loopback.getHostAddress());
            configuraciones.setPuerto(receptor.getLocalPort());

            try (BandClient cliente = new BandClient()) {
                cliente.playBass(36, 500);

                byte[] datos = new byte[512];
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);
                receptor.receive(paquete);

                assertEquals("/s_new", leerCadenaOsc(paquete));
            }
        }
    }

    @Test
    void closePropagaErrorDeLiberacionDespuesDeCerrarElSocket()
            throws Exception {
        BandClient cliente = new BandClient("127.0.0.1", 1) {
            @Override
            public void allNotesOff() throws IOException {
                throw new IOException("fallo de cierre");
            }
        };

        assertThrows(IOException.class, cliente::close);
        cliente.close();
    }

    private static String leerCadenaOsc(DatagramPacket paquete) {
        int longitud = 0;
        while (longitud < paquete.getLength()
                && paquete.getData()[paquete.getOffset() + longitud] != 0) {
            longitud++;
        }
        return new String(paquete.getData(), paquete.getOffset(),
                longitud, StandardCharsets.US_ASCII);
    }
}
