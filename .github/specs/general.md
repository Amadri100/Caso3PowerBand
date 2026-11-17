---
id: general
type: spec
scope: general
title: Arquitectura general del proyecto
status: draft
version: 0.2.0
owner: Equipo PowerBand
created: 2026-11-17
updated: 2026-11-17
depends_on: []
---

# Proyecto

PowerBand es una aplicación Java basada en Maven que transforma entradas de
micrófono, control de juegos y tableta en señales para sintetizadores locales
y remotos.

## Funciones

- Capturar y procesar audio desde el micrófono.
- Leer entradas de un control compatible.
- Leer posición y presión del lápiz desde una tableta compatible.
- Convertir las entradas en comandos de interpretación.
- Ejecutar acciones sobre uno o varios sintetizadores.
- Cambiar el dispositivo de entrada asociado a un instrumento.
- Cambiar el sintetizador asociado a un instrumento.
- Establecer y administrar la conexión con la banda.
- Buscar, reproducir y guardar grabaciones.
- Registrar eventos y errores de la aplicación.

## Paquetes

### util

#### Constantes

- Clase final con constructor privado.
- Contiene las constantes compartidas por el programa.

#### Configuraciones

- Clase final de configuración global.
- Expone las opciones modificables de la aplicación.

#### Otros

- Clases y utilidades reutilizadas por dos o más paquetes.

### logging

- Registro de eventos, grabación de sesiones y manejo centralizado de logs.

### archivos

- Gestión de archivos de grabación y aplicación de los algoritmos de procesamiento.

### input

#### Clases de entrada

- Cada fuente de entrada se ejecuta en su propio hilo.
- Las entradas se envían a los instrumentos como flujos de datos y eventos.
- El micrófono usa la información capturada para generar señales musicales.

### instrumentos

#### Instrumento abstracto

- Contiene una referencia configurable a un sintetizador; puede ser nula.
- Aplica el patrón Bridge para desacoplar el instrumento de cada síntesis concreta.
- Expone métodos para enviar acciones al sintetizador y reemplazarlo.

#### Clases concretas

- Cada clase observa las entradas compatibles con su instrumento.
- Microfono.
- Control.
- Tableta.

### comandos

- Representa las acciones que puede ejecutar un instrumento.
- Se aplica sobre el sintetizador siguiendo el patrón Command.
- No contiene lógica propia de presentación.

### sintetizadores

- Contiene las interfaces y adaptadores de los sintetizadores.
- Recibe eventos de los instrumentos y los traduce al protocolo correspondiente.

#### BandClient

- Cliente UDP existente para comunicarse con SuperCollider/scsynth mediante mensajes OSC.
- Utiliza el host y puerto configurados y puede enviar y cerrar notas.
- La comunicación de transporte no sustituye el patrón Bridge de los instrumentos.

### controlador

- Punto de acceso global mediante una única instancia.
- Coordina los servicios necesarios sin contener lógica exclusiva de UI.

### ui

- Proporciona controles para iniciar y finalizar la conexión.
- Permite modificar configuraciones.
- Permite cambiar el dispositivo compatible asignado a un instrumento.
- Permite cambiar el sintetizador asignado a un instrumento.
- Permite buscar, localizar y guardar grabaciones.
- La UI no implementa reglas de negocio ni llamadas directas a las librerías de entrada.

### main

- Punto de arranque de la aplicación y composición de sus componentes.

## Tecnología usada

### Maven

- Se utiliza Maven para compilar el proyecto y administrar sus dependencias.
- El proyecto usa Java 17.

### Instrumentos

#### Micrófono

- TarsosDSP captura y procesa el audio.
- Los resultados se entregan al componente de instrumentos como eventos.

#### Control de juegos

- JInput detecta dispositivos y lee sus controles.
- La normalización de entradas se realiza fuera de la UI.

#### Tableta

- JPen obtiene la posición y la presión del lápiz.
- La posición y presión se normalizan antes de convertirse en comandos.
