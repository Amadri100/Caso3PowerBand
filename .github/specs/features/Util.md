---
id: util
type: feature-spec
title: Util
status: completed
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-18
updated: 2026-11-18
depends_on:
  - T-004
---

# Objetivo

Definir las clases de utilidad necesarias para centralizar configuración,
constantes, rutas de datos y registro de eventos.

# Paquete util

## util.Constantes

Crear una clase final con constructor privado.

Debe centralizar:

- El puerto UDP predeterminado `57117`.
- Las rutas compartidas de logs y grabaciones.
- Los tamaños fijos de los registros persistentes.
- Las categorías de log `comando`, `info` y `error`.
- Las claves o nombres constantes usados por más de una clase.

No se permiten valores constantes compartidos quemados en varias clases. Los
componentes de entrada, sintetizadores, archivos y logging consultan esta clase.

## util.Configuraciones

Crear una clase final para la configuración global de la aplicación.

Debe proporcionar acceso común a:

- El host del servidor de sintetizadores.
- El puerto UDP del servidor.
- Las opciones modificables definidas por la aplicación.

El cliente de sintetizadores debe consultar esta configuración y no conservar
valores independientes del host o puerto.

La interfaz puede mostrar o modificar únicamente las opciones que se consideren
configurables. Las rutas internas y las constantes estructurales no forman parte
de las opciones editables por la persona usuaria.

# Registro de eventos

Crear un componente de logging bajo el paquete `logging`.

- Los logs se crean bajo `powerband/logs`.
- Las grabaciones musicales se almacenan en una ruta distinta de los logs.
- Cada entrada incluye categoría y momento.
- Las entradas incluyen la representación textual mediante `toString()` del
  objeto registrado.
- Las categorías válidas son `comando`, `info` y `error`.

## Eventos registrados

Registrar:

- Comandos enviados al servidor.
- Desconexiones de dispositivos.
- Valores rechazados por estar fuera de rango.
- Nombres duplicados de grabación.
- Creación de archivos.
- Fallos de inicio y ejecución del servidor.
- Errores de lectura o escritura de archivos.

Los errores de archivos deben incluir su ubicación cuando esté disponible.

# Comportamiento ante fallos

- Al desconectarse un dispositivo, se desactiva únicamente ese dispositivo.
- Las demás entradas continúan activas.
- Si el servidor no inicia, se informa el error y la aplicación vuelve al estado
  inicial.
- Si el servidor se detiene durante una grabación, se detiene y limpia la
  grabación activa.
- Después de detener o limpiar una grabación por fallo del servidor, la
  aplicación vuelve al estado inicial.

# Elementos registrables y serializables

- Cada elemento que se persiste o registra con datos propios debe tener una
  clase representativa.
- La clase debe implementar una representación textual `toString()`.
- La clase debe validar el tamaño de sus datos.
- La clase debe rechazar la serialización si los datos no cumplen el tamaño
  fijo esperado.
- El tamaño válido se define como constante en `util.Constantes`.
- La serialización produce un registro de longitud fija.

La implementación específica de los registros de grabación se define en T-007.

# Criterios de aceptación

- `util.Constantes` centraliza constantes compartidas y usa un constructor
  privado.
- `util.Configuraciones` ofrece el host y puerto comunes.
- El puerto predeterminado es `57117`.
- `BandClient` utiliza la configuración común sin duplicar valores de conexión.
- Los logs se escriben en `powerband/logs`.
- Las categorías permiten diferenciar `comando`, `info` y `error`.
- Los objetos registrados pueden representarse mediante `toString()`.
- Los registros serializables validan su tamaño antes de escribir.
- Una desconexión individual no detiene las demás entradas.
- Los fallos del servidor aplican el retorno al estado inicial descrito.
