---
id: instrucciones-y-comando
type: feature-spec
title: Instrucciones y comando
status: completed
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-18
updated: 2026-11-18
depends_on:
  - T-003
---

# Objetivo

Especificar la implementación de los modelos de instrumento y comando,
incluyendo los contratos públicos, la relación entre ambos y la información
necesaria para ejecutar una acción musical.

# Patrón Bridge

- Crear una abstracción común para los sintetizadores en el paquete
  `sintetizadores`.
- Crear una clase abstracta de instrumento en el paquete `instrumentos`.
- El instrumento conserva una referencia configurable a la abstracción común del
  sintetizador.
- La referencia puede ser nula y puede reemplazarse durante la ejecución.
- El instrumento delega la ejecución de acciones a través de la abstracción,
  sin conocer implementaciones concretas ni llamadas OSC.
- Los instrumentos concretos reciben datos de sus entradas y los interpretan
  como instrucciones musicales.

# Patrón Command

- Crear un contrato común de comando en el paquete `comandos`.
- Cada instrucción concreta encapsula el sintetizador destino y los parámetros
  necesarios para ejecutar la acción.
- La ejecución se realiza mediante un método único del contrato de comando,
  que delega al sintetizador correspondiente.
- Los comandos no deben capturar eventos de teclado, ratón, UI o dispositivos.
- Los comandos no deben conocer el servidor UDP ni construir mensajes OSC.

# Tipos de acciones

Las acciones deben distinguirse según su comportamiento:

- **Acción puntual:** ejecuta una sola operación con sus parámetros.
- **Inicio sostenido:** inicia una acción que continúa hasta recibir su finalización.
- **Actualización sostenida:** modifica los parámetros de una acción ya iniciada.
- **Finalización sostenida:** termina una acción iniciada por el instrumento.

Cada tipo puede implementarse como una clase concreta o como una instrucción
parametrizada, siempre que conserve:

- El sintetizador de destino.
- Los datos musicales necesarios.
- Los datos suficientes para identificar una acción sostenida.

# Relación entre instrumentos y sintetizadores

- Un instrumento puede enviar acciones a más de un sintetizador.
- Un sintetizador puede recibir acciones de más de un instrumento.
- La asignación se realiza desde el controlador o la UI, pero la lógica de
  asignación no se coloca dentro de los comandos.
- Reemplazar un sintetizador no obliga a modificar el instrumento abstracto ni
  los contratos compartidos.

# Elementos serializables

- Cada dato registrado como parte de una sesión debe tener una clase propia.
- El modelo de comandos debe exponer una representación textual mediante
  `toString()`.
- Cuando el dato se registre en un archivo, su tamaño fijo debe validarse antes
  de aceptar la operación.
- Las constantes de tamaños se consultan desde `util.Constantes`.
- La implementación concreta del formato persistido pertenece a T-007.

# Criterios de aceptación

- Los instrumentos dependen de una abstracción de sintetizador, no de una
  implementación concreta.
- La asignación de sintetizador puede cambiar sin modificar las clases
  concretas de instrumentos.
- Cada comando tiene una acción ejecutable y conserva su destino y parámetros.
- Las acciones sostenidas tienen operaciones de inicio, actualización y
  finalización.
- Agregar un sintetizador no obliga a modificar la jerarquía de instrumentos.
- El modelo no contiene código de UI, entrada de dispositivos ni transporte OSC.
