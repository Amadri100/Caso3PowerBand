package powerband.input;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jpen.PLevel;
import jpen.PLevelEvent;
import jpen.PenDevice;
import jpen.PenManager;
import jpen.PenProvider;
import jpen.event.PenAdapter;
import jpen.event.PenManagerListener;
import jpen.provider.wintab.WintabProvider;

import powerband.instrumentos.Tableta;
import powerband.util.Constantes;
import powerband.util.DispositivoEntrada;

public class EntradaTableta extends Entrada {

    private final Tableta tableta;
    private final Canvas superficie = new Canvas();
    private final Map<Byte, PenDevice> dispositivosDetectados = new ConcurrentHashMap<>();
    private volatile PenManager manager;
    private volatile String dispositivoSeleccionado;
    private volatile boolean modoDescubrimiento = true;
    private volatile Consumer<String> observadorRetiro;

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
        superficie.setBackground(Color.WHITE);
    }

    public Component superficie() {
        return superficie;
    }

    public synchronized List<DispositivoEntrada> dispositivosCompatibles() {
        if (!superficie.isShowing()) {
            throw new IllegalStateException("La superficie de la tableta no esta visible");
        }
        if (manager == null) {
            try {
                PenManager nuevo = new PenManager(superficie);
                nuevo.pen.addListener(new ListenerTableta());
                nuevo.addListener(new PenManagerListener() {
                    @Override
                    public void penDeviceAdded(PenProvider.Constructor constructor, PenDevice dispositivo) {
                    }

                    @Override
                    public void penDeviceRemoved(PenProvider.Constructor constructor, PenDevice dispositivo) {
                        if (dispositivosDetectados.remove(dispositivo.getId(), dispositivo)) {
                            Consumer<String> observador = observadorRetiro;
                            if (observador != null) {
                                observador.accept(identificador(dispositivo));
                            }
                        }
                    }
                });
                manager = nuevo;
            } catch (LinkageError fallo) {
                throw new IllegalStateException("No se pudo cargar JPen para la tableta", fallo);
            }
        }
        if (manager.getProvider(WintabProvider.class) == null) {
            throw new IllegalStateException(
                    "No se pudo iniciar Wintab; compruebe el controlador de la tableta");
        }
        List<PenDevice> dispositivos = dispositivosDetectados.values().stream()
                .filter(dispositivo -> dispositivo.getProvider() instanceof WintabProvider)
                .filter(PenDevice::getEnabled)
                .filter(dispositivo -> dispositivo.getPhysicalId() != null
                        && !dispositivo.getPhysicalId().isBlank())
                .toList();
        Map<String, Long> cantidades = dispositivos.stream()
                .collect(Collectors.groupingBy(EntradaTableta::identificador,
                        Collectors.counting()));
        return dispositivos.stream()
                .filter(dispositivo -> cantidades.get(identificador(dispositivo)) == 1)
                .map(dispositivo -> new DispositivoEntrada(identificador(dispositivo),
                        dispositivo.getName() + " (" + dispositivo.getPhysicalId() + ")"))
                .toList();
    }

    public void seleccionarDispositivo(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la tableta no puede estar vacio");
        }
        dispositivoSeleccionado = id;
    }

    public void limpiarDispositivoSeleccionado() {
        dispositivoSeleccionado = null;
    }

    public void establecerModoDescubrimiento(boolean activo) {
        modoDescubrimiento = activo;
    }

    public void registrarObservadorRetiro(Consumer<String> observador) {
        observadorRetiro = Objects.requireNonNull(observador, "observador");
    }

    private static String identificador(PenDevice dispositivo) {
        return "tableta:" + dispositivo.getPhysicalId();
    }

    public double[] normalizarPosicion(double posicionX,
                                       double posicionY,
                                       double presion) {
        Rectangle area = areaCompletaPantallas();
        validarValor("posicionX", posicionX, area.getMinX(), area.getMaxX());
        validarValor("posicionY", posicionY, area.getMinY(), area.getMaxY());
        validarValor("presion", presion, Constantes.PRESION_MINIMA,
                Constantes.PRESION_MAXIMA);
        return new double[] {
                NormalizadorEntrada.normalizar(posicionX, area.getMinX(), area.getMaxX(), 0.0, 1.0),
                NormalizadorEntrada.normalizar(posicionY, area.getMinY(), area.getMaxY(), 0.0, 1.0),
                presion
        };
    }

    public void procesarPosicion(double posicionX, double posicionY, double presion) {
        double[] normalizados = normalizarPosicion(posicionX, posicionY, presion);
        if (tableta != null) {
            tableta.convertirPosicion(normalizados[0], normalizados[1], normalizados[2]);
        }
    }

    public static Rectangle areaCompletaPantallas() {
        Rectangle area = new Rectangle();
        for (GraphicsDevice dispositivo
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            area = area.union(dispositivo.getDefaultConfiguration().getBounds());
        }
        return area;
    }

    @Override
    protected void ejecutarCaptura() throws InterruptedException {
        while (estaActiva()) {
            Thread.sleep(10);
        }
    }

    void procesarMuestra(String id, float x, float y, float presion, int ancho, int alto) {
        if (!estaActiva() || modoDescubrimiento || id == null
                || !id.equals(dispositivoSeleccionado)
                || ancho <= 0 || alto <= 0 || x < 0 || y < 0 || x > ancho || y > alto) {
            return;
        }
        validarValor("presion", presion, Constantes.PRESION_MINIMA, Constantes.PRESION_MAXIMA);
        if (tableta != null) {
            tableta.convertirPosicion(x / ancho, y / alto, presion);
        }
    }

    private final class ListenerTableta extends PenAdapter {
        @Override
        public void penLevelEvent(PLevelEvent evento) {
            PenDevice dispositivo = evento.getDevice();
            if (dispositivo == null || !(dispositivo.getProvider() instanceof WintabProvider)) {
                return;
            }
            dispositivosDetectados.put(dispositivo.getId(), dispositivo);
            String id = identificador(dispositivo);
            long coincidencias = dispositivosDetectados.values().stream()
                    .filter(actual -> identificador(actual).equals(id)).count();
            if (coincidencias != 1) {
                return;
            }
            float x = evento.pen.getLevelValue(PLevel.Type.X);
            float y = evento.pen.getLevelValue(PLevel.Type.Y);
            float presion = evento.pen.getLevelValue(PLevel.Type.PRESSURE);
            int ancho = superficie.getWidth();
            int alto = superficie.getHeight();
            if (!superficie.isShowing()) {
                return;
            }
            try {
                procesarMuestra(id, x, y, presion, ancho, alto);
            } catch (RuntimeException excepcion) {
                if (estaActiva()) {
                    desconectar(excepcion.getMessage(), excepcion);
                }
            }
        }
    }
}
