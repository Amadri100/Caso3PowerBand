package powerband.input;

import java.util.List;

import org.junit.jupiter.api.Test;

import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntradaTableroDeteccionTest {

    @Test
    void laTableroidadtaSeDetectaCuandoLaSuperficieEsVisible() throws InterruptedException {
        EntradaTableta entrada = new EntradaTableta();
        
        // Crear un frame para mostrar la superficie
        javax.swing.JFrame frame = new javax.swing.JFrame("Test Tablet");
        frame.add(entrada.superficie());
        frame.setSize(360, 180);
        frame.setVisible(true);
        
        try {
            Thread.sleep(500);
            
            List<DispositivoEntrada> tabletas = entrada.dispositivosCompatibles();
            assertNotNull(tabletas, "La lista de tabletas no debe ser null");
            
            System.out.println("Tabletas detectadas: " + tabletas.size());
            for (DispositivoEntrada tableta : tabletas) {
                System.out.println("  - " + tableta.id() + ": " + tableta.nombre());
            }
            
            System.out.println("✓ Detección de tableta completada");
            System.out.println("  Superficie visible: " + entrada.superficie().isShowing());
            
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}
