---
id: SynthDefs
type: reference
title: Definiciones y carga de sintetizadores en SuperCollider
status: reference
version: 1.0.0
owner: Equipo PowerBand
created: 2026-11-19
updated: 2026-11-19
depends_on:
  - T-103
---

# Definiciones verificadas

El archivo `myband.sc` contiene las siguientes definiciones:

| SynthDef | Función musical | Duración y cierre |
|---|---|---|
| `classicPiano` | Acorde de piano construido desde una nota MIDI, tercera y quinta. | Usa `durMs` y termina mediante `doneAction: 2`. |
| `stringBass` | Nota de bajo pulsada construida desde una nota MIDI. | Usa `durMs` y termina mediante `doneAction: 2`. |
| `organ70s` | Órgano con frecuencia, tres razones y tres amplitudes de parciales. | Mantiene la nota mientras `gate` está activo; `gate = 0` inicia su liberación. |

La función `~myband[\defs]` define y añade los tres SynthDefs al servidor.
`~myband[\load]` arranca el servidor seleccionado, evalúa esas definiciones y
espera la sincronización antes de publicar el mensaje de carga.

# Cargar los SynthDefs

1. Abrir `myband.sc` en SuperCollider IDE.
2. Evaluar el bloque de código entre paréntesis que define `~myband`, por
   ejemplo con `Ctrl+Enter` mientras el cursor está dentro del bloque.
3. Antes de iniciar el servidor, seleccionar el puerto que usa PowerBand. El
   valor predeterminado de `Configuraciones` es `57117`; SuperCollider usa
   normalmente `57110` para el servidor local predeterminado. Para que el
   servidor local reciba los mensajes del cliente con el puerto predeterminado,
   evaluar primero:

   ```supercollider
   Server.default = Server(
       \powerband,
       NetAddr("127.0.0.1", 57117),
       ServerOptions.new
   );
   ```

4. Evaluar `~myband.load;`. La función inicia el servidor si hace falta,
   registra los SynthDefs y espera `Server.default.sync`. Confirmar en la
   ventana Post el mensaje:

   ```text
   myband: classicPiano, stringBass, organ70s loaded
   ```

5. Para una comprobación sonora manual opcional, evaluar `~myband.test;`. Esto
   reproduce una demostración y no forma parte de las pruebas unitarias.

El orden importa: cambiar el servidor predeterminado después de ejecutar
`~myband.load` no mueve definiciones ni mensajes al nuevo servidor. Si el puerto
se cambia en PowerBand, configurar el servidor SuperCollider con el mismo
destino antes de cargar las definiciones.

# Contrato que esperan los adaptadores Java

- `classicPiano`: `/s_new` con `note`, `durMs` y `third`; `third` usa 4
  semitonos para mayor y 3 para menor.
- `stringBass`: `/s_new` con `note` y `durMs`.
- `organ70s`: `/s_new` con `freq`, `r1`, `r2`, `r3`, `a1`, `a2` y `a3`; el
  control `gate` inicia en 1.
- El cierre de una nota de órgano se envía por `/n_set` con el node ID de la
  nota activa y `gate = 0`.

# Validación

- Confirmar que `myband.sc` define los tres nombres de SynthDef indicados.
- Cargar las definiciones en el servidor seleccionado y verificar el mensaje
  de sincronización de carga.
- Comprobar que el puerto de escucha de `scsynth` coincide con el puerto
  configurado en `Configuraciones`.
- Ejecutar la demostración opcional y comprobar piano, bajo, inicio de órgano y
  cierre de notas.

La prueba sonora es manual y requiere SuperCollider y una salida de audio. No
se ejecuta desde `mvn test` ni implica que UDP confirme la recepción de cada
mensaje.

# Referencia de API

- [Server](https://doc.sccode.org/Classes/Server.html) describe el servidor de
  SuperCollider y su dirección de red.
- [ServerOptions](https://doc.sccode.org/Classes/ServerOptions.html) describe
  las opciones usadas al iniciar el proceso servidor.
