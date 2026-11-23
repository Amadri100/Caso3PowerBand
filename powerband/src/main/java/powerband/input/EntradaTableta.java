package powerband.input;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Canvas;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import powerband.instrumentos.Tableta;
import powerband.util.Constantes;

public class EntradaTableta extends Entrada {

    private final Tableta tableta;
    private volatile Object managerJPen;
    private volatile Object listenerJPen;

    public EntradaTableta() {
        this((Tableta) null, null);
    }

    public EntradaTableta(Tableta tableta) {
        this(tableta, null);
    }

    public EntradaTableta(Tableta tableta,
                          Consumer<Entrada> callbackDesconexion) {
        super("tableta", callbackDesconexion);
        this.tableta = tableta;
    }

    public double[] normalizarPosicion(double posicionX,
                                       double posicionY,
                                       double presion) {
        Rectangle area = areaCompletaPantallas();
        validarValor("posicionX", posicionX,
                area.getMinX(), area.getMaxX());
        validarValor("posicionY", posicionY,
                area.getMinY(), area.getMaxY());
        validarValor("presion", presion,
                Constantes.PRESION_MINIMA,
                Constantes.PRESION_MAXIMA);

        double x = NormalizadorEntrada.normalizar(
                posicionX, area.getMinX(), area.getMaxX(), 0.0, 1.0);
        double y = NormalizadorEntrada.normalizar(
                posicionY, area.getMinY(), area.getMaxY(), 0.0, 1.0);
        return new double[] {x, y, presion};
    }

    public void procesarPosicion(double posicionX,
                                 double posicionY,
                                 double presion) {
        double[] normalizados =
                normalizarPosicion(posicionX, posicionY, presion);
        if (tableta != null) {
            tableta.convertirPosicion(normalizados[0],
                    normalizados[1], normalizados[2]);
        }
    }

    public static Rectangle areaCompletaPantallas() {
        Rectangle area = new Rectangle();
        for (GraphicsDevice dispositivo
                : GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getScreenDevices()) {
            area = area.union(dispositivo.getDefaultConfiguration()
                    .getBounds());
        }
        return area;
    }

    @Override
    protected void ejecutarCaptura() throws Exception {
        try {
            Class<?> claseManager = Class.forName("jpen.PenManager");
            Canvas lienzo = new Canvas();
            managerJPen = claseManager
                    .getConstructor(java.awt.Component.class)
                    .newInstance(lienzo);
            Object pen = claseManager.getField("pen").get(managerJPen);
            Class<?> claseListener = Class.forName("jpen.event.PenListener");

            listenerJPen = Proxy.newProxyInstance(
                    claseListener.getClassLoader(),
                    new Class<?>[] {claseListener},
                    new ListenerJPen());
            pen.getClass().getMethod("addListener", claseListener)
                    .invoke(pen, listenerJPen);
            while (estaActiva()) {
                Thread.sleep(10);
            }
        } catch (ClassNotFoundException excepcion) {
            while (estaActiva()) {
                Thread.sleep(10);
            }
        }
    }

    @Override
    protected void liberarRecursosEspecificos() {
        Object manager = managerJPen;
        Object listener = listenerJPen;
        if (manager != null && listener != null) {
            try {
                Object pen = manager.getClass().getField("pen").get(manager);
                Class<?> claseListener =
                        Class.forName("jpen.event.PenListener");
                pen.getClass().getMethod("removeListener", claseListener)
                        .invoke(pen, listener);
            } catch (ReflectiveOperationException excepcion) {
                // La liberacion de JPen depende de recursos que pueden
                // haber sido desconectados por el sistema operativo.
            }
        }
        managerJPen = null;
        listenerJPen = null;
    }

    private final class ListenerJPen implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if ("penLevelEvent".equals(method.getName())
                    && args != null
                    && args.length == 1) {
                Object evento = args[0];
                Object pen = evento.getClass().getSuperclass()
                        .getField("pen").get(evento);
                Class<?> claseTipo =
                        Class.forName("jpen.PLevel$Type");
                Object tipoX = claseTipo.getField("X").get(null);
                Object tipoY = claseTipo.getField("Y").get(null);
                Object tipoPresion =
                        claseTipo.getField("PRESSURE").get(null);
                float x = ((Number) pen.getClass()
                        .getMethod("getLevelValue", claseTipo)
                        .invoke(pen, tipoX)).floatValue();
                float y = ((Number) pen.getClass()
                        .getMethod("getLevelValue", claseTipo)
                        .invoke(pen, tipoY)).floatValue();
                float presion = ((Number) pen.getClass()
                        .getMethod("getLevelValue", claseTipo)
                        .invoke(pen, tipoPresion)).floatValue();
                try {
                    procesarPosicion(x, y, presion);
                } catch (RuntimeException excepcion) {
                    if (estaActiva()) {
                        desconectar(excepcion.getMessage(), excepcion);
                    } else {
                        throw excepcion;
                    }
                }
            }
            return null;
        }
    }
}
