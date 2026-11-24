package powerband.ui;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.junit.jupiter.api.Test;

import powerband.controlador.FachadaControlador;
import powerband.controlador.ObservadorEstado;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.DispositivoEntrada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PanelInstrumentosTest {

    @Test
    public void conservaLaFachadaYCargaDatosSinModificarAsignaciones()
            throws Exception {
        AtomicInteger asignaciones = new AtomicInteger();
        AtomicReference<PanelInstrumentos> panelReferencia =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarInstrumentos")) {
                return List.of("piano", "bajo");
            }
            if (metodo.getName().equals("listarDispositivos")) {
                return List.of(new DispositivoEntrada("teclado", "teclado"),
                        new DispositivoEntrada("gamepad", "gamepad"));
            }
            if (metodo.getName().equals("listarSintetizadores")) {
                return List.of(
                        IdSintetizador.PIANO,
                        IdSintetizador.BAJO);
            }
            if (metodo.getName().equals("dispositivoPorInstrumento")) {
                return Map.of("piano", "teclado");
            }
            if (metodo.getName().equals("sintetizadoresPorInstrumento")) {
                return Map.of(
                        "piano",
                        List.of(IdSintetizador.PIANO,
                                IdSintetizador.BAJO));
            }
            if (metodo.getName().equals("asignarDispositivo")
                    || metodo.getName().equals(
                            "reemplazarSintetizadores")) {
                asignaciones.incrementAndGet();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(metodo.getReturnType())) {
                return Map.of();
            }
            return null;
        });

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelInstrumentos(fachada)));
        esperarDisponible(panelReferencia.get().botonDispositivo());

        SwingUtilities.invokeAndWait(() -> {
            assertSame(fachada, panelReferencia.get().fachada());
            assertEquals("piano",
                    panelReferencia.get().listaInstrumentos()
                            .getSelectedValue());
            assertEquals(new DispositivoEntrada("teclado", "teclado"),
                    panelReferencia.get().selectorDispositivo()
                            .getSelectedItem());
            assertEquals(2,
                    panelReferencia.get().listaSintetizadores()
                            .getSelectedIndices().length);
        });
        assertEquals(0, asignaciones.get());
    }

    
    @Test
    @SuppressWarnings("unchecked")
    public void reemplazaElDispositivoYElConjuntoDeSintetizadores()
            throws Exception {
        AtomicReference<String> instrumentoDispositivo =
                new AtomicReference<>();
        AtomicReference<String> dispositivoAsignado =
                new AtomicReference<>();
        AtomicReference<String> instrumentoSintetizadores =
                new AtomicReference<>();
        AtomicReference<List<IdSintetizador>> sintetizadoresAsignados =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarInstrumentos")) {
                return List.of("piano");
            }
            if (metodo.getName().equals("listarDispositivos")) {
                return List.of(new DispositivoEntrada("teclado", "teclado"),
                        new DispositivoEntrada("gamepad", "gamepad"));
            }
            if (metodo.getName().equals("listarSintetizadores")) {
                return List.of(
                        IdSintetizador.PIANO,
                        IdSintetizador.BAJO);
            }
            if (metodo.getName().equals("dispositivoPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getName().equals("sintetizadoresPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getName().equals("asignarDispositivo")) {
                instrumentoDispositivo.set((String) argumentos[0]);
                dispositivoAsignado.set((String) argumentos[1]);
                return true;
            }
            if (metodo.getName().equals("reemplazarSintetizadores")) {
                instrumentoSintetizadores.set((String) argumentos[0]);
                sintetizadoresAsignados.set(
                        new ArrayList<>((List<IdSintetizador>)
                                argumentos[1]));
                return true;
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(metodo.getReturnType())) {
                return Map.of();
            }
            return null;
        });
        AtomicReference<PanelInstrumentos> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelInstrumentos(fachada)));
        esperarDisponible(panelReferencia.get().botonSintetizadores());

        SwingUtilities.invokeAndWait(() -> {
            PanelInstrumentos panel = panelReferencia.get();
            panel.selectorDispositivo().setSelectedItem(
                    new DispositivoEntrada("gamepad", "gamepad"));
            panel.botonDispositivo().doClick();
        });
        esperarDisponible(panelReferencia.get().botonDispositivo());

        SwingUtilities.invokeAndWait(() -> {
            PanelInstrumentos panel = panelReferencia.get();
            panel.listaSintetizadores().setSelectedIndices(
                    new int[] {0, 1});
            panel.botonSintetizadores().doClick();
        });
        esperarDisponible(panelReferencia.get().botonDispositivo());

        assertEquals("piano", instrumentoDispositivo.get());
        assertEquals("gamepad", dispositivoAsignado.get());
        assertEquals("piano", instrumentoSintetizadores.get());
        assertEquals(
                List.of(IdSintetizador.PIANO, IdSintetizador.BAJO),
                sintetizadoresAsignados.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void permiteVaciarLaSeleccionDeSintetizadores()
            throws Exception {
        AtomicReference<List<IdSintetizador>> sintetizadoresAsignados =
                new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarInstrumentos")) {
                return List.of("piano");
            }
            if (metodo.getName().equals("listarDispositivos")) {
                return List.of();
            }
            if (metodo.getName().equals("listarSintetizadores")) {
                return List.of(IdSintetizador.PIANO);
            }
            if (metodo.getName().equals("dispositivoPorInstrumento")) {
                return Map.of();
            }
            if (metodo.getName().equals("sintetizadoresPorInstrumento")) {
                return Map.of(
                        "piano", List.of(IdSintetizador.PIANO));
            }
            if (metodo.getName().equals("reemplazarSintetizadores")) {
                sintetizadoresAsignados.set(
                        new ArrayList<>((List<IdSintetizador>)
                                argumentos[1]));
                return true;
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            if (List.class.isAssignableFrom(metodo.getReturnType())) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(metodo.getReturnType())) {
                return Map.of();
            }
            return null;
        });
        AtomicReference<PanelInstrumentos> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelInstrumentos(fachada)));
        esperarDisponible(panelReferencia.get().botonSintetizadores());
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(panelReferencia.get().mensajeEstado().getText()
                    .contains("No hay dispositivos compatibles"));
            assertTrue(!panelReferencia.get().botonDispositivo().isEnabled());
            panelReferencia.get().listaSintetizadores().clearSelection();
            panelReferencia.get().botonSintetizadores().doClick();
        });
        esperarDisponible(panelReferencia.get().botonSintetizadores());

        assertNotNull(sintetizadoresAsignados.get());
        assertTrue(sintetizadoresAsignados.get().isEmpty());
    }

    @Test
    public void integraUnaVistaUnificadaEnPanelPrincipal()
            throws Exception {
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarInstrumentos")
                    || metodo.getName().equals("listarDispositivos")
                    || metodo.getName().equals("listarSintetizadores")
                    || metodo.getName().equals("listarSesiones")
                    || metodo.getName().equals("buscarSesiones")) {
                return List.of();
            }

            if (metodo.getName().equals("dispositivoPorInstrumento")
                    || metodo.getName().equals(
                            "sintetizadoresPorInstrumento")
                    || metodo.getName().equals("opcionesConfigurables")) {
                return Map.of();
            }
            if (metodo.getReturnType() == boolean.class) {
                return false;
            }
            return null;
        });
        AtomicReference<PanelPrincipal> panelReferencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                panelReferencia.set(new PanelPrincipal(fachada)));

        SwingUtilities.invokeAndWait(() -> {
            assertInstanceOf(
                    PanelInstrumentos.class,
                    panelReferencia.get()
                            .panelVista(Vista.INSTRUMENTOS));
            assertEquals(6, panelReferencia.get().cantidadVistas());
        });
    }

    @Test
    void descartaLaRespuestaAnteriorAlCambiarDeInstrumento() throws Exception {
        CountDownLatch primeraIniciada = new CountDownLatch(1);
        CountDownLatch liberarPrimera = new CountDownLatch(1);
        CountDownLatch primeraFinalizada = new CountDownLatch(1);
        AtomicReference<PanelInstrumentos> referencia = new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            if (metodo.getName().equals("listarInstrumentos")) {
                return List.of("primero", "segundo");
            }
            if (metodo.getName().equals("listarDispositivos")) {
                if ("primero".equals(argumentos[0])) {
                    primeraIniciada.countDown();
                    try {
                        if (!liberarPrimera.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("No se liberó la primera consulta");
                        }
                    } catch (InterruptedException excepcion) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(excepcion);
                    }
                    return List.of(new DispositivoEntrada("antiguo", "Antiguo"));
                }
                return List.of(new DispositivoEntrada("nuevo", "Nuevo"));
            }
            if (metodo.getName().equals("listarSintetizadores")) {
                return List.of();
            }
            if (Map.class.isAssignableFrom(metodo.getReturnType())) {
                return Map.of();
            }
            return null;
        });

        SwingUtilities.invokeAndWait(() -> referencia.set(new PanelInstrumentos(fachada)));
        assertTrue(primeraIniciada.await(5, TimeUnit.SECONDS));
        try {
            SwingUtilities.invokeAndWait(() -> {
                SwingWorker<?, ?> primera = referencia.get().consultaActual();
                primera.addPropertyChangeListener(evento -> {
                    if ("state".equals(evento.getPropertyName())
                            && evento.getNewValue() == SwingWorker.StateValue.DONE) {
                        primeraFinalizada.countDown();
                    }
                });
                referencia.get().listaInstrumentos().setSelectedIndex(1);
            });
            esperarDisponible(referencia.get().botonSintetizadores());
            liberarPrimera.countDown();
            assertTrue(primeraFinalizada.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals("segundo", referencia.get().listaInstrumentos().getSelectedValue());
                assertEquals(new DispositivoEntrada("nuevo", "Nuevo"),
                        referencia.get().selectorDispositivo().getItemAt(0));
                assertEquals(1, referencia.get().selectorDispositivo().getItemCount());
            });
        } finally {
            liberarPrimera.countDown();
        }
    }
    @Test
    void actualizaLaListaAlRecibirUnaDesconexion() throws Exception {
        AtomicReference<ObservadorEstado> observador = new AtomicReference<>();
        AtomicReference<List<DispositivoEntrada>> disponibles = new AtomicReference<>(
                List.of(new DispositivoEntrada("audio", "Micrófono")));
        AtomicReference<Map<String, String>> asignaciones =
                new AtomicReference<>(Map.of("microfono", "audio"));
        AtomicReference<PanelInstrumentos> referencia = new AtomicReference<>();
        FachadaControlador fachada = fachada((metodo, argumentos) -> {
            return switch (metodo.getName()) {
                case "listarInstrumentos" -> List.of("microfono");
                case "listarDispositivos" -> disponibles.get();
                case "listarSintetizadores" -> List.of();
                case "dispositivoPorInstrumento" -> asignaciones.get();
                case "sintetizadoresPorInstrumento" -> Map.of();
                case "registrarObservadorEstado" -> {
                    observador.set((ObservadorEstado) argumentos[0]);
                    yield null;
                }
                case "retirarObservadorEstado" -> {
                    observador.set(null);
                    yield null;
                }
                default -> null;
            };
        });

        SwingUtilities.invokeAndWait(() -> {
            referencia.set(new PanelInstrumentos(fachada));
            referencia.get().addNotify();
        });
        try {
            esperarDisponible(referencia.get().botonDispositivo());
            disponibles.set(List.of());
            asignaciones.set(Map.of());
            observador.get().dispositivoDesconectado("microfono");
            SwingUtilities.invokeAndWait(() -> { });
            esperarDisponible(referencia.get().botonSintetizadores());
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(0, referencia.get().selectorDispositivo().getItemCount());
                assertTrue(!referencia.get().botonDispositivo().isEnabled());
                assertTrue(referencia.get().mensajeEstado().getText()
                        .contains("No hay dispositivos compatibles"));
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> referencia.get().removeNotify());
        }
        assertEquals(null, observador.get());
    }

    private static FachadaControlador fachada(
            BiFunction<Method, Object[], Object> manejador) {
        return (FachadaControlador) Proxy.newProxyInstance(
                FachadaControlador.class.getClassLoader(),
                new Class<?>[] {FachadaControlador.class},
                (proxy, metodo, argumentos) ->
                        manejador.apply(metodo, argumentos));
    }

    private static void esperarDisponible(
            javax.swing.AbstractButton boton)
            throws Exception {
        CountDownLatch disponible = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            if (boton.isEnabled()) {
                disponible.countDown();
            } else {
                boton.addPropertyChangeListener(
                        "enabled", evento -> {
                            if (Boolean.TRUE.equals(evento.getNewValue())) {
                                disponible.countDown();
                            }
                        });
            }
        });
        assertTrue(disponible.await(5, TimeUnit.SECONDS));
    }
}
