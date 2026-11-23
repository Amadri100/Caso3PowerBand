---
id: parteA
type: tasks
status: in-progress
updated: 2026-11-23
---



## Sintetizadores y transporte

- [x] T-101 | Inventariar los métodos de `BandClient` (envío de `/s_new`, `/n_set`, cierre) y anotarlos como referencia | status: done | deps: R-001
- [x] T-102 | Alinear `BandClient` con `Configuraciones` (host y puerto `57117`, sin valores propios) | status: done | deps: R-005, T-101
- [x] T-103 | Verificar que los SynthDefs `classicPiano`, `stringBass` y `organ70s` están definidos y documentar cómo cargarlos en `scsynth` | status: done | deps: R-001
- [x] T-104 | Adaptador `PianoSintetizador`: `playPiano` con nota MIDI, duración en ms y tercera (mayor por defecto) | status: done | deps: R-008, T-102
- [x] T-105 | Adaptador `BajoSintetizador`: `playBass` con nota MIDI y duración en ms | status: done | deps: R-008, T-102
- [x] T-106 | Adaptador `OrganoSintetizador`: `noteOn` (nota a frecuencia, razones 1, 2, 3 y amplitudes 1, 0.5, 0.25 por defecto) | status: done | deps: R-008, T-102
- [x] T-107 | `OrganoSintetizador`: `noteOff` (`/n_set` con `gate = 0`) y `allNotesOff`, llevando el registro de notas activas | status: done | deps: T-106
- [x] T-108 | Validar rangos de parámetros en los adaptadores; rechazar, notificar y registrar valores inválidos | status: done | deps: T-104, T-105, T-106, R-006
- [x] T-109 | Capturar errores locales de envío UDP, notificarlos y registrarlos; sin presentar el envío como confirmación | status: done | deps: T-102, R-006

## Comandos

- [x] T-110 | Comando puntual (`ComandoPuntual`): conserva destino y parámetros; `ejecutar()` delega al sintetizador | status: done | deps: R-009
- [x] T-111 | Comando de inicio sostenido, con datos para identificar la acción | status: done | deps: T-110
- [x] T-112 | Comando de actualización sostenida | status: done | deps: T-111
- [x] T-113 | Comando de finalización sostenida | status: done | deps: T-111
- [x] T-114 | Implementar `toString()` en los comandos y registrar cada comando enviado en la categoría `comando` | status: done | deps: T-110, R-006

## Instrumentos (Bridge)

- [x] T-115 | Clase abstracta `Instrumento`: referencia configurable a `Sintetizador` (puede ser nula), reemplazo en ejecución y envío de acciones | status: done | deps: R-008, R-010
- [x] T-116 | `Instrumento`: soporte para varios sintetizadores destino y publicación de cada comando en `BusComandos` | status: done | deps: T-115, T-114
- [x] T-117 | Instrumento concreto `Microfono`: convierte datos del micrófono en comandos | status: done | deps: T-116
- [x] T-118 | Instrumento concreto `Control`: convierte eventos del control en comandos | status: done | deps: T-116
- [x] T-119 | Instrumento concreto `Tableta`: convierte posición y presión en comandos | status: done | deps: T-116

## Entradas

- [x] T-120 | Clase base de entrada: hilo propio, iniciar/detener independiente, liberación de recursos y callback de desconexión | status: done | deps: R-006
- [x] T-121 | Utilidad de normalización de valores (rango del dispositivo a rango musical) | status: done | deps: R-001
- [x] T-122 | Entrada de micrófono: captura con Java Sound y detección de tono con TarsosDSP | status: done | deps: T-120, T-121
- [x] T-123 | Entrada de control: detectar dispositivos y leer eventos con JInput | status: done | deps: T-120, T-121
- [x] T-124 | Entrada de tableta: posición y presión con JPen, posición respecto del área completa de pantallas | status: done | deps: T-120, T-121
- [x] T-125 | Excluir teclado y mouse como dispositivos válidos | status: done | deps: T-123
- [x] T-126 | Rechazo de valores fuera de rango en las entradas, con notificación y registro | status: done | deps: T-121, R-006
- [x] T-127 | Manejo de desconexión: desactivar solo la entrada afectada, notificar y registrar | status: done | deps: T-120, R-006

## Pruebas de la Parte A

- [x] T-130 | Pruebas unitarias de comandos y adaptadores con un sintetizador falso | status: done | deps: T-113, T-107
- [x] T-131 | Pruebas: dos instrumentos a un mismo sintetizador y un instrumento a dos sintetizadores | status: done | deps: T-116
- [x] T-132 | Pruebas de entradas: inicio/detención independiente, rechazo de rangos y desconexión simulada | status: done | deps: T-127, T-126
- [ ] T-133 | Prueba manual con `scsynth`: piano, bajo y órgano (`noteOn`, `noteOff`, `allNotesOff`) | status: todo | deps: T-107, T-103
