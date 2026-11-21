package powerband.grabaciones;

@FunctionalInterface
public interface EsperaReproduccion {

    void esperar(long nanosegundos) throws InterruptedException;
}
