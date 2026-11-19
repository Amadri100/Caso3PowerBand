---
id: documentos
type: feature-spec
title: documentos
status: completed
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-18
updated: 2026-11-18
depends_on:
  - T-004
  - T-007
---

# Objetivo

Especificar los documentos usados por PowerBand, su ubicación, sus estructuras
binarias o textuales y las clases Java responsables de leerlos y escribirlos.

# Convenciones comunes

- Las rutas compartidas se declaran en `util.Constantes`.
- Los registros se usan como secuencias de bytes y no como texto separado por
  líneas.
- Los enteros y posiciones se almacenan con el orden definido por
  `util.Constantes.ORDEN_BYTES_PROGRAMA` (big-endian).
- Los campos de texto se codifican como UTF-8.
- Los nombres tienen como máximo 200 bytes. No se recorta una cadena si su
  representación UTF-8 excede el límite; el valor se rechaza.
- Los bytes sobrantes se rellenan con ceros. Al leer, los ceros finales se
  consideran relleno y no forman parte del nombre.
- Las fechas se representan como epoch millis en un `long`.
- Los tamaños fijos se validan antes de escribir o aceptar la deserialización.
- La validación y codificación de nombres UTF-8 compartida por los registros se
  implementa en `archivos.ValidacionesGenerales`.
- Toda clase de registro implementa `toString()` y valida el tamaño fijo antes
  de serializar.

# `log`

- Las entradas de log se escriben bajo `powerband/logs`.
- Este documento no usa los tamaños ni formatos de `recordings.*`.
- Una entrada contiene categoría(enum), instante y representación textual del objeto.
- Las categorías son `comando`, `info` y `error`.
- La escritura recibe el evento como objeto o texto y usa `toString()` para
  registrar su descripción.
- Una entrada debe permitir identificar qué ocurrió y cuándo ocurrió.
- Los errores de archivos incluyen la ruta o ubicación disponible.
- Se registran comandos enviados, desconexiones, valores rechazados, nombres
  duplicados, creación de archivos y fallos del servidor o del almacenamiento.
- Las entradas se pueden añadir sin modificar ni alterar las grabaciones.

# `recordings.music`

- Contiene las grabaciones como encabezados seguidos de sus comandos.
- La carpeta base es `powerband/recordings`, separada de `powerband/logs`.
- `ArchivosGrabacion.inicializar()` crea la carpeta y los archivos musicales
  faltantes sin truncar los que ya existen; registra en el log la ubicación de
  cada archivo recién creado.
- No contiene audio grabado ni muestras.
- Los encabezados y comandos usan registros de tamaño fijo.
- Un archivo inválido o con un tamaño no múltiplo de su registro se reporta
  como incompleto y no se presenta como una sesión válida.

## `EncabezadoGrabacion`

Campos y distribución:

| Campo | Tipo Java | Tamaño | Descripción |
|---|---|---:|---|
| Nombre | `String` | 200 bytes | Nombre único codificado UTF-8 y rellenado con ceros |
| Posición inicial | `long` | 8 bytes | Primer byte ocupado por la grabación |
| Posición final | `long` | 8 bytes | Primer byte posterior al último registro |
| Fecha | `long` | 8 bytes | Epoch millis de la grabación |

Tamaño fijo acordado: **224 bytes**.

- La posición inicial y final son offsets absolutos en bytes dentro de
  `recordings.music`.
- La posición final excluye el intervalo posterior al último comando.
- El nombre no puede exceder 200 bytes UTF-8.
- Una clase llamada `EncabezadoGrabacion` representa, valida y serializa estos
  cuatro campos.

## `ComandoGrabado`

Campos y distribución:

| Campo | Tipo Java | Tamaño | Descripción |
|---|---|---:|---|
| Tiempo relativo | `long` | 8 bytes | Milisegundos desde el inicio de la grabación |
| Identificador de sintetizador | `byte` | 1 byte | Piano, bajo u órgano |
| Tipo de acción | `byte` | 1 byte | Puntual, inicio, actualización o finalización |
| Cantidad de parámetros | `byte` | 1 byte | Cantidad válida de valores utilizados |
| Parámetros | `double[8]` | 64 bytes | Valores en posiciones consecutivas |

Tamaño fijo acordado: **75 bytes**.

Valores válidos de sintetizador:

- `1`: piano.
- `2`: bajo.
- `3`: órgano.

Valores válidos de acción:

- `1`: puntual.
- `2`: inicio.
- `3`: actualización.
- `4`: finalización.

- Las notas sostenidas incluyen el identificador necesario para su
  finalización dentro de sus parámetros.
- La cantidad de parámetros debe estar entre `0` y `8`.
- Los parámetros no utilizados se escriben como cero.
- Las clases `IdSintetizador` y `TipoAccion` definen los valores semánticos,
  mientras que `ComandoGrabado` escribe sus códigos estables.
- `ComandoGrabado` representa el tiempo relativo, destino, tipo y parámetros;
  no contiene lógica OSC.

# `recordings.list`

- Conserva una entrada por grabación, en orden cronológico.
- Cada entrada contiene nombre y fecha/hora.
- El nombre ocupa 200 bytes UTF-8 y la fecha ocupa 8 bytes epoch millis.
- Tamaño fijo acordado de `EntradaListaGrabacion`: **208 bytes**.
- Las fechas se comparan por su epoch millis.
- Si dos fechas son iguales, el nombre se compara con
  `archivos.ComparadorNombres`; nombres iguales según el comparador son
  duplicados.
- `archivos.EntradaListaGrabacion` representa cada registro, y la gestión de
  inserción/listado está en `archivos.ListaGrabaciones` según
  `../parteB/T-207-T-208.md`.
- El archivo se lee secuencialmente por registros; no se carga su contenido
  binario completo en un único arreglo.

# `recordings.index`

- Conserva el nombre y las posiciones inicial/final de cada grabación.
- Mantiene las entradas ordenadas por nombre para permitir búsqueda binaria.
- El ordenamiento y la búsqueda usan el criterio único definido en
  `archivos.ComparadorNombres`, basado en `String.CASE_INSENSITIVE_ORDER`,
  independiente del locale y sin normalización Unicode.
- Los nombres que el comparador considera iguales también cuentan como
  duplicados; la forma original del nombre se conserva en el registro.
- `archivos.EntradaIndice` se busca directamente en disco mediante búsqueda
  binaria con `archivos.BuscadorIndice`, según el contrato de
  `../parteB/T-207-T-208.md`.

## `EntradaIndice`

Campos y distribución:

| Campo | Tipo Java | Tamaño | Descripción |
|---|---|---:|---|
| Nombre | `String` | 200 bytes | Nombre único ordenado y rellenado |
| Posición inicial | `long` | 8 bytes | Offset inicial en `recordings.music` |
| Posición final | `long` | 8 bytes | Offset final en `recordings.music` |

Tamaño fijo acordado: **216 bytes**.

- `EntradaIndice` valida que el nombre tenga como máximo 200 bytes UTF-8.
- `EntradaIndice` valida y serializa posiciones como `long`.
- Un índice se considera válido solo si su tamaño es múltiplo de 216 bytes.
- La búsqueda binaria opera directamente sobre registros del archivo.
- El criterio de comparación usado por búsqueda binaria debe ser el mismo que usa
  Balanced Merge Sort.
- `archivos.OrdenadorIndice` realiza el ordenamiento externo definido en
  `../parteB/T-209.md`: opera sobre archivos temporales y mantiene la memoria
  acotada; T-209 no reemplaza el índice original.

# Operaciones sobre archivos

- Si un archivo requerido no existe, `ArchivosGrabacion` lo crea vacío bajo
  `Constantes.RUTA_GRABACIONES` y registra su ubicación. Los nombres de los
  archivos se declaran en `util.Constantes`.
- Antes de leer, se valida que el tamaño del archivo sea múltiplo del registro
  correspondiente.
- Si existe residuo, el archivo se informa como incompleto o inválido.
- Las lecturas y escrituras usan offsets absolutos en bytes alineados con el
  tamaño del registro; la cantidad se calcula solo después de comprobar que no
  haya residuo. `archivos.ArchivoRegistros` implementa este acceso genérico;
  el contrato completo está en `../parteB/T-204-T-205.md`.
- Un registro con nombre vacío, código de sintetizador desconocido o tipo de
  acción desconocido se rechaza.
- Al guardar una grabación se verifica primero que el nombre no exista.
- Un nombre duplicado rechaza la operación sin modificar la música ni los
  índices existentes.
- Una grabación incompleta se elimina ante un error y se registra el fallo.
- Un nuevo índice solo reemplaza al anterior después de terminar y validar el
  ordenamiento.
- Las posiciones inicial/final de `recordings.index` deben describir un intervalo
  válido dentro de `recordings.music`.

# Carga y reproducción

- `recordings.list` permite mostrar las sesiones en orden cronológico.
- El nombre seleccionado se busca en `recordings.index`.
- Las posiciones del índice localizan el encabezado y los comandos dentro de
  `recordings.music`.
- Los comandos se leen en disco y se envían al sintetizador según su tiempo
  relativo.
- La reproducción conserva el intervalo desde el inicio registrado y termina
  las acciones sostenidas necesarias.
- No se carga toda la música en arreglos ni se reproduce como audio.

# Criterios de aceptación

- Los campos y tamaños de los tres registros quedan definidos sin valores quemados
  en el código.
- Los tamaños fijos son 224 bytes para `EncabezadoGrabacion`, 75 bytes para
  `ComandoGrabado`, 208 bytes para `EntradaListaGrabacion` y 216 bytes para
  `EntradaIndice`.
- Las fechas y tiempos relativos se almacenan como epoch millis o milisegundos
  relativos en `long`.
- Las posiciones se almacenan como offsets de bytes en `long`.
- Los registros binarios se validan antes de deserializar o serializar.
- El nombre se limita a 200 bytes UTF-8 y se rechaza si excede el límite.
- `recordings.list` usa orden cronológico y `recordings.index` usa búsqueda
  binaria y Balanced Merge Sort en disco.
- Los archivos faltantes se crean y se registra su ubicación.
- Los duplicados y errores no reemplazan ni alteran datos válidos anteriores.