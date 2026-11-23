---
id: BandClient
type: reference
title: Inventario del cliente OSC existente
status: reference
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-19
updated: 2026-11-19
depends_on:
  - T-101
---

# Propósito

Registrar el comportamiento que ya ofrece `powerband.sintetizadores.BandClient`
como referencia para los adaptadores de sintetizador y las tareas de transporte.
Este documento describe el código existente; no define una API nueva ni realiza
cambios en la implementación.

# Conexión y recursos

- `BandClient` implementa `AutoCloseable`.
- `BandClient()` obtiene el host y puerto de `Configuraciones.getInstance()`.
  El valor inicial es `127.0.0.1:57117`.
- `BandClient(Configuraciones configuraciones)` permite proporcionar una
  configuración compartida explícita.
- `BandClient(String host, int port)` abre un `DatagramSocket`, resuelve la
  dirección del host y conserva el puerto de destino indicado; se mantiene
  para pruebas y usos que necesitan un destino explícito.
- El socket usa un puerto local asignado por el sistema; `port` es el puerto
  remoto al que se envían los mensajes.
- El cliente no conserva valores predeterminados independientes de
  `Configuraciones`.

# API pública de acciones musicales

| Método | Comportamiento | Mensaje OSC |
|---|---|---|
| `playPiano(int note, int durMs)` | Ejecuta el acorde de piano mayor por defecto. | `/s_new` con `classicPiano`, `note`, `durMs` y `third = 4`. |
| `playPiano(int note, int durMs, boolean major)` | Ejecuta el acorde mayor o menor. | `/s_new` con `classicPiano`; `third` vale `4` para mayor y `3` para menor. |
| `playBass(int note, int durMs)` | Ejecuta una nota de bajo con duración en milisegundos. | `/s_new` con `stringBass`, `note` y `durMs`. |
| `noteOn(int note)` | Inicia una nota de órgano con parciales predeterminados. | `/s_new` con `organ70s`; razones `1, 2, 3` y amplitudes `1, 0.5, 0.25`. |
| `noteOn(int note, float[] ratios, float[] amps)` | Inicia una nota de órgano con tres razones y tres amplitudes. Rechaza arreglos cuyo tamaño no sea tres. | `/s_new` con `organ70s`, frecuencia convertida desde MIDI y controles `r1..r3`, `a1..a3`. |
| `noteOff(int note)` | Detiene la nota de órgano asociada a esa nota MIDI, si está activa. | `/n_set` con el node ID correspondiente y `gate = 0`. |
| `allNotesOff()` | Solicita el cierre de todas las notas de órgano que la instancia mantiene registradas. | Ejecuta `noteOff` por cada nota activa. |
| `close()` | Intenta cerrar todas las notas y luego cierra el socket. | Propaga `IOException` si falla localmente el envío de cierre; cerrar el socket no confirma recepción remota. |

# Comportamiento interno relevante

- Los mensajes `/s_new` asignan un node ID consecutivo a partir de
  `FIRST_NODE_ID` (`10000`), usan `ADD_TO_HEAD` (`0`) y el grupo destino
  `DEFAULT_GROUP` (`1`).
- `activeOrgan` asocia cada nota MIDI activa con el node ID que se creó para
  ella. Antes de iniciar otra instancia de la misma nota, `noteOn` invoca
  `noteOff` para la nota previa.
- `noteOff` elimina una nota del registro local solo después de enviar su
  mensaje de cierre sin error local.
- `allNotesOff` intenta cerrar cada nota registrada aunque falle alguno de los
  envíos y propaga el primer `IOException`, con los fallos adicionales como
  excepciones suprimidas.
- `midiToFreq(int note)` convierte una nota MIDI a frecuencia con la referencia
  de afinación de 440 Hz.
- El helper `sNew(String defName, Object... controls)` construye los argumentos
  de `/s_new` y devuelve el node ID local asignado.
- `send(String path, Object... args)` codifica y envía un mensaje OSC por UDP.
  Actualmente acepta argumentos `Integer`, `Float` y `String`; los tipos
  numéricos de 32 bits se escriben en orden big-endian.
- `writeString` codifica cadenas como ASCII, añade el terminador nulo y rellena
  el campo hasta un múltiplo de cuatro bytes, como requiere OSC.
- `writeInt` escribe un entero de 32 bits en orden big-endian.

# Métodos de demostración

- `main(String[] args)` es un ejemplo manual, no una operación para invocar
  desde la interfaz. Acepta host y puerto opcionales, ejecuta ejemplos de
  piano, bajo y órgano, y espera entre mensajes con `Thread.sleep`.
- El ejemplo requiere un `scsynth` en ejecución y los SynthDefs cargados. No
  constituye una prueba unitaria ni una confirmación automática de que el
  servidor recibió o ejecutó cada mensaje.

# Errores y límites observados

- Los métodos que envían mensajes pueden propagar `IOException` ante un error
  local de transporte.
- UDP no proporciona confirmación de recepción ni de ejecución. Que
  `DatagramSocket.send` no falle localmente no demuestra que `scsynth` procesó
  el mensaje.
- `close()` cierra el socket incluso si falla el intento local de liberar notas
  y luego propaga el `IOException`; el llamador puede registrar y comunicar el
  fallo. Esto no confirma si el servidor recibió los mensajes.
- El cierre de una nota solo envía `/n_set` cuando la nota está registrada en
  `activeOrgan`; no envía un cierre remoto para notas que la instancia no
  conoce.

# Uso como referencia para las siguientes tareas

- T-102 alineó la creación predeterminada del cliente con el host y puerto de
  `Configuraciones`.
- T-104 a T-107 implementan los adaptadores `PianoSintetizador`,
  `BajoSintetizador` y `OrganoSintetizador` sobre los métodos públicos de esta
  clase.
- T-109 registra y propaga los errores locales sin presentar el envío UDP como
  confirmación remota.
- La tabla describe los métodos y mensajes tras la implementación de T-102 a
  T-109; cualquier modificación posterior debe actualizar esta referencia si
  cambia su comportamiento.
