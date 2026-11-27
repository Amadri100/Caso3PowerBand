package powerband.excepciones;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import powerband.excepciones.tipos.TipoErrorArchivo;
import powerband.excepciones.tipos.TipoErrorArgumento;
import powerband.excepciones.tipos.TipoErrorEstado;
import powerband.excepciones.tipos.TipoErrorOrdenamiento;
import powerband.util.Constantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcepcionesTest {

    @Test
    void codigosDeEnumsSonUnicosYTienenPrefijo() {
        Set<String> codigos = new HashSet<>();
        verificarTipos(TipoErrorArchivo.values(), "ARC-", codigos);
        verificarTipos(TipoErrorOrdenamiento.values(), "ORD-", codigos);
        verificarTipos(TipoErrorArgumento.values(), "ARG-", codigos);
        verificarTipos(TipoErrorEstado.values(), "EST-", codigos);
    }

    @Test
    void plantillasSePuedenFormatear() {
        assertTrue(TipoErrorArchivo.ARCHIVO_INCOMPLETO.formatear(
                217, 216, 1).contains("residuo"));
        assertTrue(TipoErrorOrdenamiento.NOMBRE_DUPLICADO.formatear(
                "Ana", "ana").contains("Ana"));
        assertTrue(TipoErrorArgumento.OFFSET_INVALIDO.formatear(
                216, -216).contains("-216"));
        assertTrue(TipoErrorEstado.TEMPORAL_AJENO.formatear()
                .contains("temporales"));
    }

    @Test
    void exponeMensajeGenericoParaExcepcionesNoCapturadas() {
        assertEquals("Excepcion no capturada",
                ConstantesExcepciones.MSG_EXCEPCION_NO_CAPTURADA);
    }

    @Test
    void excepcionesExponenDatosYCausaCompatible() {
        ArchivoIncompletoException incompleta =
                new ArchivoIncompletoException(
                        "indice", 217, Constantes.TAMANO_ENTRADA_INDICE);
        assertEquals(1, incompleta.residuo());
        assertTrue(incompleta.codigo().startsWith("ARC-"));
        assertTrue(incompleta instanceof java.io.IOException);

        NombreDuplicadoException duplicada =
                new NombreDuplicadoException("Ana", "ana", "indice");
        assertEquals("Ana", duplicada.nombre1());
        assertEquals("ana", duplicada.nombre2());
        assertTrue(duplicada instanceof OrdenamientoException);

        ArgumentoInvalidoException argumento =
                ArgumentoInvalidoException.offsetInvalido(216, -216);
        assertTrue(argumento instanceof IllegalArgumentException);
        assertInstanceOf(TipoErrorArgumento.class, argumento.tipo());
    }

    private void verificarTipos(CodigoError[] tipos, String prefijo,
            Set<String> codigos) {
        for (CodigoError tipo : tipos) {
            assertTrue(tipo.codigo().startsWith(prefijo));
            assertTrue(codigos.add(tipo.codigo()),
                    "Código duplicado: " + tipo.codigo());
            assertTrue(tipo.plantilla() != null
                    && !tipo.plantilla().isBlank());
        }
    }
}
