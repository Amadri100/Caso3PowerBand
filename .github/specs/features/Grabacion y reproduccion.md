---
id: grabacion-y-reproduccion
type: feature-spec
title: Grabacion y reproduccion
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

Definir la estructura y las operaciones de almacenamiento necesarias para
grabar, buscar y reproducir sesiones sin guardar audio.

# Archivos

## recordings.music

- Contiene los encabezados y los comandos de las grabaciones.
- Cada encabezado identifica el nombre, la fecha y las posiciones inicial y
  final expresadas en bytes.
- Cada comando conserva su tiempo relativo al inicio, el sintetizador invocado
  y sus parámetros.
- Los encabezados y comandos se representan en registros binarios de tamaño fijo.

## recordings.list

- Conserva nombre y fecha/hora de cada grabación.
- Mantiene las grabaciones en orden cronológico.
- Se utiliza para mostrar las sesiones disponibles en la interfaz.

## recordings.index

- Conserva nombre y posiciones inicial/final dentro de `recordings.music`.
- Mantiene los nombres ordenados para permitir búsqueda por nombre.
- Las posiciones se almacenan y consultan como posiciones de bytes.
- Cada entrada tiene tamaño fijo.

# Registro de clases

- Crear clases específicas para encabezados, comandos grabados y entradas del
  índice.
- Cada clase ofrece una representación textual mediante `toString()`.
- Cada clase valida que sus datos permitan formar el tamaño fijo definido.
- El tamaño fijo esperado se declara en `util.Constantes`.
- La serialización rechaza datos cuyo tamaño no coincida con la constante.
- La cantidad de registros se calcula dividiendo el tamaño del archivo entre el
  tamaño fijo del registro.
- Si existe residuo, el archivo se informa como incompleto o inválido.

# Grabación

- Al iniciar, se registra el tiempo base de la sesión.
- Cada comando enviado se escribe con su tiempo relativo, sintetizador y
  parámetros.
- No se captura ni se almacena audio.
- Al detener, se solicita un nombre único.
- Si el nombre existe, se rechaza sin modificar la sesión previa ni actualizar
  el índice.
- Solo después de una grabación válida se agrega su encabezado y se actualizan
  los archivos de lista e índice.
- Si ocurre un error, se elimina la sesión incompleta y se registra el error.
- Un resultado incompleto o inválido no reemplaza un índice válido anterior.

# Índice y búsqueda

- La búsqueda por nombre se realiza en disco mediante búsqueda binaria.
- No se cargan todos los registros del índice en arreglos de memoria.
- El índice se ordena con Balanced Merge Sort sobre almacenamiento.
- El criterio de comparación usado por el ordenamiento debe coincidir
  exactamente con el criterio usado por la búsqueda.
- Un índice nuevo reemplaza al anterior únicamente después de completar el
  ordenamiento y validar el resultado.
- Las posiciones recuperadas se usan para leer el intervalo correspondiente
  dentro de `recordings.music`.

# Carga y reproducción

- La carga obtiene las grabaciones de `recordings.list` en orden cronológico.
- Al seleccionar una grabación, se busca su nombre en `recordings.index`.
- Las posiciones obtenidas permiten localizar el encabezado y los comandos.
- Los comandos se reproducen mediante buffering en el orden registrado.
- Los tiempos relativos se respetan desde el inicio de la grabación.
- Las acciones sostenidas se reproducen con sus comandos de finalización.
- No se interpreta el archivo como audio grabado.

# Errores

- Registrar nombre duplicado, registros incompletos, tamaños inesperados,
  archivos inválidos y fallos de lectura o escritura.
- Si falta un archivo de datos, crear el archivo requerido y registrar su
  ubicación.
- Un error de archivo no se presenta como operación exitosa.
- Si se detiene el servidor durante una grabación, detener y limpiar la sesión
  incompleta conforme a T-004 y T-008.

# Criterios de aceptación

- Las sesiones guardan comandos y tiempos relativos, no audio.
- Los nombres duplicados conservan la grabación existente.
- `recordings.list` presenta grabaciones en orden cronológico.
- La búsqueda usa búsqueda binaria en disco y el índice usa Balanced Merge Sort
  sin cargar todos los registros en memoria.
- El encabezado y los comandos se almacenan en registros de tamaño fijo.
- El tamaño del archivo debe ser múltiplo del tamaño fijo; de lo contrario se
  informa como inválido.
- Las posiciones del índice identifican bytes dentro de `recordings.music`.
- La reproducción conserva el orden y los intervalos relativos registrados.
- Las notas sostenidas finalizan mediante los comandos registrados.
- Los errores no reemplazan datos válidos ni presentan operaciones fallidas como
  exitosas.
