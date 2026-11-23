package powerband.main;

import java.io.IOException;

import javax.swing.SwingUtilities;

import powerband.controlador.Controlador;
import powerband.controlador.FachadaControlador;
import powerband.sintetizadores.BandClient;
import powerband.ui.VentanaPrincipal;
import powerband.util.Configuraciones;
import powerband.util.Constantes;

public class Main {

    public static void main(String[] args) throws Exception {
        if (esDemostracionSintetizadores(args)) {
            ejecutarDemostracionSintetizadores(args);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana =
                    new VentanaPrincipal(crearFachada());
            ventana.setVisible(true);
        });
    }

    static boolean esDemostracionSintetizadores(String[] args) {
        return args != null
                && args.length > 0
                && Constantes.ARGUMENTO_DEMO_SINTETIZADORES
                        .equalsIgnoreCase(args[0]);
    }

    static void ejecutarDemostracionSintetizadores(String[] args)
            throws IOException, InterruptedException {
        Configuraciones configuraciones = Configuraciones.getInstance();
        String host = args.length > 1
                ? args[1]
                : configuraciones.getHost();
        int puerto = args.length > 2
                ? Integer.parseInt(args[2])
                : configuraciones.getPuerto();

        try (BandClient cliente = new BandClient(host, puerto)) {
            System.out.println(
                    "Demostracion T-133 con scsynth en "
                            + host + ":" + puerto);

            cliente.playPiano(48, 1500);
            esperarMilisegundos(1700);
            cliente.playPiano(57, 1500, false);
            esperarMilisegundos(1700);
            cliente.playPiano(53, 2000);
            esperarMilisegundos(2300);

            for (int nota : new int[] {36, 43, 45, 41}) {
                cliente.playBass(nota, 450);
                esperarMilisegundos(500);
            }

            cliente.noteOn(60);
            esperarMilisegundos(1500);
            cliente.noteOn(64,
                    new float[] {1f, 2.01f, 4f},
                    new float[] {1f, 0.6f, 0.3f});
            esperarMilisegundos(1500);
            cliente.noteOn(67,
                    new float[] {1f, 3f, 5f},
                    new float[] {1f, 0.4f, 0.2f});
            esperarMilisegundos(2000);

            cliente.noteOff(60);
            cliente.noteOff(64);
            cliente.allNotesOff();
            esperarMilisegundos(1000);

            System.out.println(
                    "Demostracion local completada. "
                            + "Escucha la salida de audio para validar T-133.");
        }
    }

    private static void esperarMilisegundos(long milisegundos)
            throws InterruptedException {
        Thread.sleep(milisegundos);
    }

    static FachadaControlador crearFachada() {
        return Controlador.getInstance();
    }
}
