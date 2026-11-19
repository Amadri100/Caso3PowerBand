package powerband.archivos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.comandos.TipoAccion;
import powerband.logging.LoggerPowerBand;
import powerband.sintetizadores.IdSintetizador;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComandoGrabadoTest {

    @TempDir
    Path carpetaTemporal;

    @AfterEach
    void restaurarRutaLogs() {
        LoggerPowerBand.cambiarPath(Path.of(Constantes.RUTA_LOGS));
    }

    @Test
    void serializaYLeeElFormatoFijoConParametrosRellenos() {
        ComandoGrabado comando = new ComandoGrabado(
                350, IdSintetizador.ORGANO, TipoAccion.INICIO,
                new double[] {440.0, 0.5});

        byte[] datos = comando.serializar();
        ComandoGrabado leido = ComandoGrabado.desdeBytes(datos);
        ByteBuffer buffer = ByteBuffer.wrap(datos)
                .order(ByteOrder.BIG_ENDIAN);

        assertEquals(75, datos.length);
        assertEquals(350, buffer.getLong());
        assertEquals(3, Byte.toUnsignedInt(buffer.get()));
        assertEquals(2, Byte.toUnsignedInt(buffer.get()));
        assertEquals(2, Byte.toUnsignedInt(buffer.get()));
        assertEquals(0.0, buffer.getDouble(11 + 2 * Double.BYTES));
        assertEquals(350, leido.tiempoRelativoMillis());
        assertEquals(IdSintetizador.ORGANO, leido.idSintetizador());
        assertEquals(TipoAccion.INICIO, leido.tipoAccion());
        assertArrayEquals(new double[] {440.0, 0.5}, leido.parametros());
        assertTrue(leido.toString().contains("ORGANO"));
    }

    @Test
    void protegeElArregloDeParametros() {
        double[] originales = {60.0, 0.75};
        ComandoGrabado comando = new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL, originales);
        originales[0] = 99.0;

        double[] expuestos = comando.parametros();
        expuestos[1] = 0.0;

        assertArrayEquals(new double[] {60.0, 0.75}, comando.parametros());
    }

    @Test
    void rechazaTiempoCantidadYLongitudesInvalidas() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoGrabado(
                        -1, IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                        new double[0]));
        assertThrows(IllegalArgumentException.class,
                () -> ComandoGrabado.desdeBytes(new byte[74]));

        LoggerPowerBand.cambiarPath(carpetaTemporal);
        assertThrows(IllegalArgumentException.class,
                () -> new ComandoGrabado(
                        0, IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                        new double[9]));
        String log = Files.readString(
                carpetaTemporal.resolve("powerband.log"),
                StandardCharsets.UTF_8);
        assertTrue(log.contains("Cantidad de parametros"));
    }

    @Test
    void rechazaCodigosYRegistrosConParametrosNoUtilizados() {
        byte[] codigoSintetizadorInvalido = new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL, new double[0])
                .serializar();
        codigoSintetizadorInvalido[8] = 99;
        assertThrows(IllegalArgumentException.class,
                () -> ComandoGrabado.desdeBytes(codigoSintetizadorInvalido));

        byte[] codigoAccionInvalido = new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL, new double[0])
                .serializar();
        codigoAccionInvalido[9] = 99;
        assertThrows(IllegalArgumentException.class,
                () -> ComandoGrabado.desdeBytes(codigoAccionInvalido));

        byte[] cantidadInvalida = new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL, new double[0])
                .serializar();
        cantidadInvalida[10] = 9;
        LoggerPowerBand.cambiarPath(carpetaTemporal);
        assertThrows(IllegalArgumentException.class,
                () -> ComandoGrabado.desdeBytes(cantidadInvalida));

        byte[] rellenoInvalido = new ComandoGrabado(
                0, IdSintetizador.PIANO, TipoAccion.PUNTUAL,
                new double[] {1.0}).serializar();
        ByteBuffer.wrap(rellenoInvalido).order(ByteOrder.BIG_ENDIAN)
                .putDouble(11 + Double.BYTES, 2.0);
        assertThrows(IllegalArgumentException.class,
                () -> ComandoGrabado.desdeBytes(rellenoInvalido));
    }
}
