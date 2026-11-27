# Preparación en Windows

El proyecto utiliza Java 21 y Maven. Desde `powerband`, ejecute `mvn test`
para compilar el proyecto y extraer las bibliotecas nativas en
`powerband/target/natives`. En VS Code, abra la raíz del repositorio y use
la configuración **PowerBand (Windows)** para iniciar la aplicación con ese
directorio en `java.library.path`.

Para la integración de tabletas se utiliza JPen 2-150301, compatible con
Java 21. Su DLL para Windows x64 requiere un controlador de tableta compatible
con Wintab. Los artefactos se obtienen del repositorio histórico de QuPath,
fijado a un commit en el POM; ese repositorio no publica sumas de verificación
Maven para estos artefactos.

## Preparación de SuperCollider

PowerBand envía OSC por UDP a `127.0.0.1:57117` por defecto. Abra
`myband.sc` en SuperCollider IDE y evalúe el bloque entre paréntesis. Después,
evalúe `~myband.load;` y espere el mensaje
`myband: classicPiano, stringBass, organ70s loaded`. Puede evaluar
`~myband.test;` para comprobar el audio directamente en SuperCollider.

El puerto del servidor y el de PowerBand deben coincidir. Si ya hay un
servidor iniciado en otro puerto, deténgalo desde SuperCollider antes de
iniciar el servidor configurado en `myband.sc`. El estado **Conectado** de
PowerBand no confirma que SuperCollider haya recibido los mensajes UDP.

## Información de depuración

En **Configuración**, active o desactive **Información de depuración del log**
y pulse **Guardar**. La opción inicia desactivada en cada ejecución y afecta a
los eventos posteriores; no desactiva el registro normal. Cuando está activa,
el log añade el componente que registró el evento y, si hay una excepción, su
tipo, causas y ubicación inicial. Cada evento ocupa una sola línea; los saltos
de línea dentro de sus datos aparecen escapados.

El log se escribe en `powerband/logs/powerband.log` relativo al directorio desde
el que se inició la aplicación. La información de depuración puede incluir
rutas y mensajes de error del entorno local; revísela antes de compartirla.
Si no es posible escribir el log durante una notificación de desconexión o
error, se informa el fallo por la salida de errores estándar y se continúa
notificando a los observadores.

## Dispositivos por instrumento

En **Instrumentos**, seleccione el instrumento, actualice la lista y asigne un
dispositivo compatible. Para la tableta, mantenga activado **Descubrir lápiz sin
emitir sonido**, toque la superficie y pulse **Actualizar dispositivos**.
Después de asignar el lápiz y un sintetizador, desactive el modo de
descubrimiento para tocar; la captura se limita a la superficie visible.

Los micrófonos se filtran por soporte para captura PCM de 44,1 kHz, 16 bits y
un canal. Los controles se filtran por tipo y componentes de JInput. Cuando la
API no permite distinguir dos dispositivos con los mismos datos de identidad,
se omiten ambos para evitar una selección equivocada.

JInput puede conservar su enumeración inicial: un control conectado después
del inicio puede requerir reiniciar la aplicación. Cada dos segundos se
comprueba la presencia de los micrófonos asignados y la respuesta de los
controles asignados que no estén capturando; durante la captura, el control
comprueba su respuesta continuamente. Un fallo de `poll()` indica que el
control no responde, no necesariamente que se haya desconectado físicamente.
Java Sound puede tardar en actualizar su lista tras conectar o retirar un
micrófono; la ausencia de sonido no se considera una desconexión.

Al detectar una retirada se elimina la asignación afectada, se notifica y se
actualiza el selector cuando corresponde. La reconexión no restaura la
asignación: vuelva a seleccionarla. Si la identidad resulta ambigua, se
notifica y no se presume una retirada. JPen/Wintab procesa las notificaciones
de retirada que reciba, pero puede conservar los lápices observados y no
garantiza avisar cuando se desconectan: su presencia en la lista no confirma
que sigan conectados. Al conectar la aplicación, la captura se inicia en
segundo plano y los fallos posteriores se notifican en la aplicación.

## Comprobación manual en Windows

Estas pruebas requieren dispositivos físicos y no forman parte de `mvn test`:

1. Conecte un control, uno o más micrófonos y una tableta con controlador
   Wintab; ejecute `mvn test` desde `powerband` e inicie **PowerBand (Windows)**
   en VS Code.
2. En **Instrumentos**, revise cada lista: el control y la tableta no deben
   aparecer en la lista del otro. Seleccione un micrófono, asígnele un
   sintetizador y conecte la aplicación a SuperCollider. Hable o cante y
   verifique que la entrada corresponda al micrófono elegido. Si tiene otro,
   repita con él.
3. Seleccione **tableta**, mantenga activado el modo de descubrimiento, toque
   la superficie y actualice la lista. Verifique que no se produzca sonido
   durante el descubrimiento. Seleccione el lápiz y asígnele un sintetizador.
4. Desactive el modo de descubrimiento y compruebe que el lápiz produce
   eventos musicales únicamente dentro de la superficie. Reactive el modo y
   confirme que deja de producirlos.
5. Asigne el control y el micrófono. Tanto con la aplicación conectada como
   desconectada de SuperCollider, retire cada dispositivo por separado y
   espere al menos unos segundos. Compruebe si se notifica, desaparece su
   asignación y se actualiza su selector, sin retirar las otras asignaciones.
   Java Sound y JInput pueden no reflejar inmediatamente estos cambios.
6. Reconecte cada dispositivo y actualice su lista. Verifique que no se
   reasigne automáticamente y asígnelo de nuevo si aparece; si el control no
   reaparece, reinicie la aplicación.
7. Retire la tableta asignada y observe si Wintab notifica su retirada.
   La notificación no está garantizada: una entrada recordada no demuestra
   que la tableta siga conectada. La inactividad del lápiz por sí sola no debe
   retirar la asignación.