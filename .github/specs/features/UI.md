---
id: ui
type: feature-spec
title: UI
status: completed
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-18
updated: 2026-11-18
depends_on:
  - T-005
  - T-006
  - T-007
  - T-008
---

# Objetivo

Definir las responsabilidades de la interfaz gráfica y su interacción con el
controlador central, sin incluir reglas de negocio ni acceso directo a servicios
de bajo nivel.

# Controlador

- Crear o usar un único punto de acceso central para coordinar la aplicación.
- El controlador compone las entradas, instrumentos, sintetizadores,
  configuración, logging y archivos de sesiones.
- El controlador inicia y detiene los servicios según las acciones solicitadas.
- La interfaz delega todas las operaciones al controlador o a servicios
  coordinados por él.
- El controlador mantiene una instancia compartida y no contiene componentes
  exclusivos de presentación.

# Funciones visibles

La interfaz debe permitir:

- Iniciar y finalizar la conexión con dispositivos y sintetizadores.
- Seleccionar el dispositivo asignado a cada instrumento.
- Cambiar la asignación entre instrumentos y sintetizadores.
- Consultar y modificar únicamente las opciones configurables.
- Iniciar y detener la grabación de una sesión.
- Consultar grabaciones en orden cronológico.
- Buscar o seleccionar una grabación y solicitar su reproducción.
- Mostrar el estado de conexión y los errores recibidos.

# Limitaciones de la UI

La interfaz no debe:

- Leer directamente micrófono, JInput o JPen.
- Crear mensajes OSC ni invocar `BandClient`.
- Ejecutar reglas de asignación o normalización de datos.
- Implementar búsqueda binaria, ordenamiento en disco o formatos de archivo.
- Modificar grabaciones directamente sin pasar por el controlador.

# Estados

- La UI no representa como exitosa una operación hasta que el servicio confirme
  su estado.
- El envío UDP exitoso solo confirma que no ocurrió un error local inmediato.
- No se presenta ese envío como confirmación de que `scsynth` recibió o ejecutó
  el comando.
- Una desconexión individual muestra el estado afectado sin ocultar las demás
  entradas.
- Los errores del servidor se presentan y conducen al estado inicial definido
  por T-004 y T-008.
- Si inicia una grabación y el servidor falla, la UI comunica el error y refleja
  que la grabación fue detenida y limpiada.

# Configuración

- Las opciones visibles se obtienen de `util.Configuraciones`.
- Las constantes compartidas se consultan desde `util.Constantes`.
- La UI no mantiene valores independientes del host y puerto usados por el
  cliente.
- Las opciones no configurables no deben exponerse como modificables.

# Ciclo de vida

- Al cerrar la aplicación se detienen todas las entradas.
- Se termina de forma controlada una reproducción activa.
- Se liberan las notas sostenidas antes de cerrar los servicios.
- Se liberan los recursos mantenidos por el cliente de transporte.
- El cierre no confirma que `scsynth` haya terminado todas las notas por sí
  mismo; la aplicación ejecuta las acciones de liberación disponibles.

# Criterios de aceptación

- Desde la UI se gestionan conexiones, asignaciones y sesiones.
- Todas las operaciones se delegan al controlador o a servicios coordinados.
- La UI no lee dispositivos, construye OSC ni implementa reglas de negocio.
- Los estados y errores corresponden a la información recibida de los servicios.
- Un envío UDP no se presenta como confirmación de ejecución del servidor.
- Una desconexión individual no oculta el estado de las demás entradas.
- Al cerrar se detienen entradas y reproducción y se liberan notas sostenidas.
- Un fallo del servidor cambia la aplicación al estado inicial y informa a la
  persona usuaria.
