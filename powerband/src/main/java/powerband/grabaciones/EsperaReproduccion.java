package powerband.grabaciones;

@FunctionalInterface
public interface EsperaReproduccion {

   public void esperar(long nanosegundos) throws InterruptedException;
}
