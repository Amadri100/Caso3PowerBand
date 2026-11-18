## Resumen del programa:
“Programa de creación e interacción musical” La idea principal es conectar varios instrumentos con varios sintetizadores. Los sintetizadores utilizan supercollider. El programa debe permitir lo siguiente, utilizar de uno a varios instrumentos con uno o varios sintetizadores. Registra estas interacciones y además permite guardarlas para una reproducción futura, en la misma aplicación (no como grabación de audio).

## Supercollider:
Es un entorno para generar y procesar audio. Permitiendo interactuar mediante código y definir matemáticamente el sonido generado.

Sample: Es una medición única de la amplitud de una onda de sonido.
Block: Cada sample se agrupa en un block.
Inicio:
Para comenzar a utilizar los sintetizadores hay que iniciar el servidor con control + b.
sclang: Lenguaje en el que se programa el audio, scsynth este es el sintetizador con el que se pueden comunicar otros lenguajes.

En el código del supercollider se tiene que “s” representa este servidor local:
También se puede utilizar: “Server.default”
s.quit;
s.boot;

También se puede utilizar: “Server.default”
f = { "Function evaluated".postln; };
f;

Así se define una función y se ejecuta así:
(
f={ arg a, b,;
  var firstResult, finalResult;
  firstResult = a + b;
  finalResult = firstResult * 2;
  finalResult
  };
f.value(2, 3); // this will return 10
)


## Generar Audio:
Se dice que se considera el audio de un sintetizador algo alto: 
{SubOsc.ar(440, 0, 0.2)}.play;
control + “.” termina la ejecución del sonido.
SinOsc es un objeto, SinOsc.ar() me da una instancia de este objeto que es un unit generator. Es la forma básica de sonido de un sintetizador y este permite utilizar diferentes cosas. SinOsc, es un oscilador que sigue una onda:

“ar” es audio rate, donde supercollider calcula un valor para cada sample.
existe “kr”: control rate que se calcula un único valor por bloque. Lo que es más eficiente y se suele usar en un contexto donde la señal controla a otra señal y no es el resultado final.

“ar”, recibe frecuencia, fase(phase) y mul. La frecuencia es el oscilador en hz, fase es donde inicia en la onda en radianes, Mul es un número que multiplica el output del ugen. 

También se pueden juntar 2 unit generators:
{FSinOsc.ar(800, 0, 0.2) + BrownNoise.ar(0.2)}.play;

Otros unit generators:
https://doc.sccode.org/Guides/Tour_of_UGens.html
## Synths: 
Un sintetizador “Synth” es una representación del lado del cliente; por lo tanto no hay que agregarlo al servidor como tal. Produce una única unidad de sonido; se define con un SynthDef.

El SynthDef es la base en la que se construye un Sintetizador específico, indica los generadores, su orden y las inputs y outputs del Synth. Synth es una subclase de nodo.
(
SynthDef(\help_isRand, { |out|
   Out.ar(out,
       SinOsc.ar(Rand(400, 800), 0, 0.2) * Line.kr(1, 0, 1, doneAction: Done.freeSelf)
   )
}).add;
)
a = Synth(\help_isRand);
b = Synth(\help_isRand);

Estés un SynthDef lo primero que se le da es un nombre.

Aquí aparecen cómo se crea el synth, primero se le da el nombre, luego los argumentos.
El target es el grupo al que afecta el synth, add action modifica la acción del add final.
Nótese que el add del final manda también esta definición al servidor.

SynthDef("help-synth", { freq = #[440, 450, 460], out = 01 Out.ar(out, Mix (Sinosc.ar(freq, 0, 0.1)));
}).add;
)
// then this a short while later
x = Synth("help-synth", [freq: [500, 501, 510]]);
x = Synth("help-synth", [freq: [500, 501, 510]]);
x.set(\freq, [1,2,3]^ * 400 + [1, 2, 3] . \out, 1);
x.set(\freq, [3]^ * 400 + [1, 2, 3] . \out, 1);
x.free;


free elimina el objeto


s.boot;
x = Synth.newPaused("default");
s.queryAllNodes; // see I'm here
x.run; // true is the default
x.run(false); // pause me again
x.free;


## Networking:
La aplicación de supercollider contiene tanto el cliente como al servidor en una.
Esta aplicación hace uso de OSC del CNMAT, usando TCP o UDP, el camino del las instrucciones desde sclang al server es: código superCollider -> traducido a OSC -> el servidor lo recibe y ejecuta. Esto causa que el código de sclang sea absurdamente de alto nivel y por ende no representa la programación que se hace en java. 

n = s.nextNodeID; // get an available nodeID from the server and assign it to n
s.sendMsg("/s_new", "default", n); // use the SynthDef "default" to create a synth with ID n
s.sendMsg("/n_free", n); // release the synth n

x = Synth("default"); // create a synth on the default server (s) and allocate an ID for it
x.free; // free the synth, its ID and resources

Por ende en ese sentido lo que necesita el servidor son las definiciones de este. El servidor lo requiere absolutamente, un programa de sclang también lo necesita lá mayor parte de las veces. Pero una app externa solo necesita el nombre.

## OSC: 
Suppercollider utiliza una versión simplificada de OSC, la que permite mandar mensajes en (TCP, UDP) de la cual usaremos UDP, la decisión es final.

scsynth -u 57117 >synth_log &

Ejecuta un scsynth, el udp en el puerto 57117 y tira la info en el archivo synth_log.
En el caso de TCP se manda un entero con el tamaño del mensaje anterior.

long | a 64 bit integer. Used for time stamps only.
int | a 32 bit integer.
float | a 32 bit single precision floating point number.
double | a 64 bit double precision floating point number.
string | an OSC format string: 8 bit ASCII characters, zero padded to a multiple of 4 bytes.
bytes | a buffer of data preceded by a 32 bit length field and padded to a multiple of 4 bytes

Hay que hacer una conversión de string UTF-8 a Ascii y mandar los datos como múltiplos de 4 bytes. Por lo tanto en argumentos más pequeños se fuerza a que sea múltiplo de 4 bytes. Además se le agrega una tag: 

'i' | an int
'f' | a float
's' | a string
'b' | bytes
'd' | a double
't' | a timestamp

using UDP
       one Bundle or one Command.
using TCP
       int32 - the length in bytes of the following message.
       one Bundle or one Command.

Podemos mandar entonces lo siguiente: 
Bundle: un conjunto de mensajes mandados para ser ejecutados en un tiempo específico, dividiendo un long en 32 bits superiores de segundos desde el 1990 y 32 bits inferiores que dividen un segundo en momentos 2^32 ticks.

## Command: Esencialmente una instrucción.

s.sendMsg(
"/s_new",
"sine",
x = s.nextNode ID,
1,
1,
"freq",
900
);

El servidor recibe un mensaje, crea un nuevo sintetizador, de nombre “sine”, con el siguiente id disponible, agregado al grupo 1, con target del grupo 1, cree el parámetro frecuencia, asígnele 900. 

s.sendBundle( 
    0.2,
    ["/s_new", "sine", x = 5 s.nextNodeID, 
    1, 1, "freq", 800],

    ["/s_new", "sine", y = s.nextNodeID, 
    1, 1, "freq", 1001],

    ["/s_new", "sine", z = s.nextNodeID, 
    1, 1, "freq", 1202]
);
s.sendBundle(time, message1, message2, message3);

Podemos ver todos los comandos posibles aquí: 
https://doc.sccode.org/Reference/Server-Command-Reference.html
Aplicación en java:
SuperCollider tiene que tener alguna definición de sintetizadores ya existentes: 

s.boot;
(
SynthDef(\simpleNote, {
    |freq 440, amp = 0.15|

    var signal;

    signal Sinosc.ar(freq) amp;

    Out.ar(0, [signal, signal]);

}).add;
)

Luego se usa el codigo del profesor xd.







## Decisión de instrumentos:
Micrófono genérico:
Decisión: javax.sound.sampled permite detectar el audio. Mientras que usamos TarsosDSP para procesar esta información.

Java nos permite manejar la detección de dispositivos.Mientras que Tarsos nos da las funciones como FFT para obtener y procesar las señales de audio y convertirlas rápidamente en datos para el sintetizador.

Control genérico genérico:
JInput, es una librería simple y sencilla que nos puede ayudar para esta función ejemplo sencillo:

Controller[] controllers ControllerEnvironment.getDefaultEnvironment().getControll
for (int i = 0 i < controllers.length; i++) {
    /* Remember to poll each one /
    controllers [i].poll();

    / Get the controllers event queue /
    EventQueue queue controllers[i].getEventQueue();
    
    / For each object in the queue */
    while (queue.getNextEvent(event)) {
        / Get event component/
        Component comp event.getComponent();
        / Process event (your awesome code) */
        ...
}
}

## Tableta:
Se utilizará JPen con ciertas modificaciones para que la app no dependa de la posición en una pantalla si no en las pantallas en general.

La otra opción sería utilizar algún driver propietario o opensource y conectarlos mediante una interfaz o clase abstracta.

## Problemas separados:
El flujo del programa son los instrumentos que son actores de los que java recibe info y lo procesa y transmite a supercollider que da el sonido como resultado.
Conexión con dispositivos:
Utiliza el driver o alguna librería de IO en java que permita utilizar todas las capacidades de dicho dispositivo.

## Editor musical:
Hace el rol del controlador para el programa, sabe todo lo que los dispositivos manda al sintetizador lo guarda y da diversas opciones.

Se usa un patrón bridge que permite conectar ‘n’ instrumentos con ‘n’ sintetizadores, no se puede que un instrumento sea para un único sintetizador. 
El instrumento tiene una referencia al sintetizador que usa si se decide cambiarlo simplemente se cambia esa referencia. Para usar el método de mandarle los datos de la interfaz general para los sintetizadores.
Conexión con el sintetizador:
Conecta y envía al servidor con los sintetizadores la información de los instrumentos para que produzca música.

Otros requerimientos:
## Trayecto:
Dispositivo -> procesamiento de datos -> socket del servidor -> sintetizador.
## Logging:
Se mantiene un log con la información que se le pasa a supercollider; aparte de las grabaciones. Las grabaciones pueden tomar la parte de este sistema que capta esto y dirigirlo para ir formando la grabación.
## Recordings y archivos:
Se tienen los siguiente archivos recordings:
.music:
Contiene las grabaciones, las que graban cada uno de los comandos mandados y tienen un encabezado con otra información.

Header: Nombre, donde inicia, donde termina, fecha
instrucciones: Momento relativo a la canción, sintetizador usado y los datos que se le mandaron al synth
Header y instrucciones del mismo tamaño fijo


.list: 
La lista de música con su nombre y fecha, Está ordenada en base a las fechas, por lo tanto no se modifica su orden.

|Nombre||Fecha| todo cabe una línea de tamaño fijo.

.index
Contiene el nombre y los índices en el recordings.music.
.
|Nombre||Inicio en recordings(Bytes)||final en recording(Bytes)|
Ordenados por nombre
Algoritmos y búsquedas:
Con el tamaño de líneas fijas podemos obtener con el tamaño de los Bytes la cantidad de líneas sin hacer ningún recorrido. Bytes/tamaño = número de líneas.

El algoritmo de ordenamiento y la búsqueda se hacen en almacenamiento. 

## Ordenamiento: Balanced Merge sort
Se toman los datos de headers de los diferentes audios y se obtienen todas las coordenadas más los nombres, luego se ponen los datos a index y se ordena. 
Es un sort externo se hace en el disco por lo que no se carga todo en memoria.


La división debe ser exacta. Si queda un residuo, el archivo está incompleto o su formato no coincide con el esperado y se debe reportar el error, no ignorar los bytes que sobran.

1. Entrada: escribir los registros del índice que se van a ordenar en un archivo de trabajo. Se puede generar primero a partir de los encabezados de `recordings.music`.
2. Distribuir corridas: repartir alternadamente los registros entre los dos archivos de entrada. Al inicio, cada registro individual es una corrida ordenada de longitud 1.
3. Mezclar: leer una corrida de cada archivo de entrada, comparar sus nombres y copiar el menor registro a uno de los archivos de salida. Al terminar la corrida, copiar cualquier registro restante de la otra. Alternar el destino de las corridas combinadas entre los dos archivos de salida.
4. Repetir rondas de mezcla: al final de una ronda, los archivos de salida se convierten en los de entrada para la siguiente. En cada ronda se duplica la longitud máxima de las corridas: 1, 2, 4, 8, etc. Se distribuyen y mezclan hasta producir una sola corrida.
5. Dar resultado: reemplazar `recordings.index` por el archivo ordenado solo cuando la salida esté completa y validada, para no perder el índice anterior cuando ocurra un fallo durante el proceso.

La memoria de trabajo se limita a buffers de lectura/escritura y a los registros que se comparan, es decir, los registros restantes permanecen en archivos. El algoritmo requiere archivos temporales en disco y varias lecturas/escrituras secuenciales.
Se ordena con un string.

## Busqueda: Busqueda binaria
No carga todo el texto igual que el algoritmo anterior.
Requiere elementos ordenados.
Toma 2 índices principio y final y revisa la mitad, para luego decidir si toma la mitad inferior o superior según el número que maneje, esto se repite hasta encontrarlo o no. 
 Alcance y uso:
El usuario puede conectar dispositivos, asignar instrumentos a sintetizadores, grabar una sesión y reproducirla.

## Especificación de archivos:

## Validaciones:
Dispositivos desconectados, valores fuera de rango, nombres duplicados, archivos inexistentes y fallos del servidor.
Valores fuera de rango: Si un valor está fuera de rango permitido, el programa debe notificar al logger y no permitir la operación.

Nombres duplicados: Si se intenta crear un instrumento o grabación con un nombre que ya existe, el programa debe notificar al logger y no permitir la operación. 

Dispositivos desconectados: Si se pierde la conexión con un dispositivo se manda una excepción al logger se desactiva el dispositivo pero el sistema sigue funcionando pues no es algo difícil de que suceda.

Archivos inexistentes: Si el archivo no existe se crea el archivo. Se indica en un log que se hizo esto y da la dirección donde se creó.

Punto inicial: Estado en el que el editor se encuentra por defecto antes de iniciar ninguna conexión.
Fallos del servidor: Todos los errores se notifican al log. Se dividen en, no inicia el servidor: Se notifica y se regresa al punto inicial. Si no afecta a la ejecución: si esta relacionado a un instrumento se informa de esto y se agrega al log no se termina la ejecución. Termina el servidor: Notifica, termina cualquier grabación y la limpia. Regresa al punto inicial.


## Instrumentos:
Un dispositivo diferente por cada uno de los 3 sintetizadores, de forma que pueda controlar y disparar los 3 synths en cualquier momento usando los instrumentos conectados a su programa.
Datos de grabaciones:
Qué guarda cada comando?
Encabezado guarda el nombre de la grabación, el tiempo de inicio y el tiempo de finalización. Cada comando guarda el instante en que ocurrió (relativo al inicio de la grabación), el synth invocado y sus parámetros. Todos los registros tienen el mismo tamaño fijo.





----------------------------------------------


+ Validaciones

Dispositivos desconectados, valores fuera de rango, nombres duplicados, archivos inexistentes y fallos del servidor.
Valores fuera de rango: Si un valor está fuera de rango permitido, el programa debe notificar al logger y no permitir la operación.

Nombres duplicados: Si se intenta crear un instrumento o grabación con un nombre que ya existe, el programa debe notificar al logger y no permitir la operación. 

Dispositivos desconectados: Si se pierde la conexión con un dispositivo se manda una excepción al logger se desactiva el dispositivo pero el sistema sigue funcionando pues no es algo difícil de que suceda.

Archivos inexistentes: Si el archivo no existe se crea el archivo. Se indica en un log que se hizo esto y da la dirección donde se creó.

Punto inicial: Estado en el que el editor se encuentra por defecto antes de iniciar ninguna conexión.
Fallos del servidor: Todos los errores se notifican al log. Se dividen en, no inicia el servidor: Se notifica y se regresa al punto inicial. Si no afecta a la ejecución: si esta relacionado a un instrumento se informa de esto y se agrega al log no se termina la ejecución. Termina el servidor: Notifica, termina cualquier grabación y la limpia. Regresa al punto inicial.


+ Instrumentos
Uso de Patrón Bridge y polimorfismo:

Se puede conectar cualquier instrumento con cualquier sintetizador. Se puede tener un instrumento conectado a varios sintetizadores y un sintetizador conectado a varios instrumentos.
- Qué sucede con un setter(nuevo synth) si el instrumento ya tiene un synth asignado?
Lo reemplaza. Si se quiere tener varios synths para un mismo instrumento se debe crear otro instrumento con el mismo tipo de dispositivo y asignarle otro synth.

Cómo se da el polimorfismo Instrumento -> Synth: Se puede conectar cualquier instrumento con cualquier sintetizador. Se puede tener un instrumento conectado a varios sintetizadores y un sintetizador conectado a varios instrumentos. Para explicarlo más detalladamente, se puede tener un instrumento conectado a varios sintetizadores y un sintetizador conectado a varios instrumentos. Esto permite una gran flexibilidad en la creación musical, ya que los músicos pueden experimentar con diferentes combinaciones de instrumentos y sintetizadores para lograr el sonido deseado.



## Desarrollo de los puntos pendientes

### Balanced merge sort para el índice

#### Propósito

`recordings.index` permite localizar una grabación por nombre mediante búsqueda binaria en disco. Para que esa búsqueda funcione, sus registros deben estar ordenados por nombre. Como el índice puede ser mayor que la memoria disponible y el caso prohíbe ordenar los registros indexados en arreglos en memoria, se utiliza un ordenamiento externo: Balanced Merge Sort.

#### Registros que se ordenan

Al finalizar una grabación, se obtiene su nombre y la posición de su encabezado en `recordings.music`, y con esos datos se forma un registro del índice. La lectura de `recordings.music` para reunir encabezados sí implica recorrer ese archivo; lo que se evita es cargar todos los registros del índice en memoria para ordenarlos.

Todos los registros de `recordings.index` deben tener un tamaño fijo conocido. Por ejemplo, cada registro puede contener un nombre de longitud fija y la posición del encabezado expresada como entero binario. El nombre es la clave de ordenamiento; la posición debe viajar junto con el nombre cada vez que el registro se mueve. Si se usa texto, se deben fijar también la codificación y el ancho en bytes. El formato binario fijo facilita calcular posiciones y evita confundir cantidad de caracteres con cantidad de bytes.

Con registros de tamaño fijo, la cantidad de registros se calcula como:

`cantidad = tamaño del archivo en bytes / tamaño de un registro en bytes`

La división debe ser exacta. Si queda un residuo, el archivo está incompleto o su formato no coincide con el esperado y se debe reportar el error, no ignorar los bytes sobrantes.


1. Entrada: escribir los registros del índice que se van a ordenar en un archivo de trabajo. Se puede generar primero a partir de los encabezados de `recordings.music`.
2. Distribuir corridas: repartir alternadamente los registros entre los dos archivos de entrada. Al inicio, cada registro individual es una corrida ordenada de longitud 1.
3. Mezclar: leer una corrida de cada archivo de entrada, comparar sus nombres y copiar el menor registro a uno de los archivos de salida. Al terminar la corrida, copiar cualquier registro restante de la otra. Alternar el destino de las corridas combinadas entre los dos archivos de salida.
4. Repetir rondas de mezcla: al final de una ronda, los archivos de salida se convierten en los de entrada para la siguiente. En cada ronda se duplica la longitud máxima de las corridas: 1, 2, 4, 8, etc. Se distribuyen y mezclan hasta producir una sola corrida.
5. Dar resultado: reemplazar `recordings.index` por el archivo ordenado solo cuando la salida esté completa y validada, para no perder el índice anterior cuando ocurra un fallo durante el proceso.

La memoria de trabajo se limita a buffers de lectura/escritura y a los registros que se comparan, es decir, los registros restantes permanecen en archivos. El algoritmo requiere archivos temporales en disco y varias lecturas/escrituras secuenciales.

#### Ejemplo

Para los nombres de entrada `D, A, C, B`, la distribución inicial alternada produce corridas de tamaño 1:

| Pasada | Entrada A | Entrada B | Corridas mezcladas |
|---|---|---|---|
| 1 | `[D] [C]` | `[A] [B]` | `[A,D]` y `[B,C]` |
| 2 | `[A,D]` | `[B,C]` | `[A,B,C,D]` |

En cada comparación se ordena por el nombre, pero se escribe el registro completo (nombre y posición del encabezado). El criterio de comparación —incluyendo mayúsculas/minúsculas y tratamiento de acentos si aplica— debe ser idéntico al de la búsqueda binaria.

#### Relación con la búsqueda binaria

Una vez ordenado el índice, si tiene `n` registros de tamaño `R`, el registro de la mitad se puede leer directamente desde la posición `medio × R`, donde `medio` es el índice del registro central. Se compara su nombre con el buscado y se continúa en la mitad inferior o superior. Esto evita cargar el archivo completo en memoria; requiere que el tamaño sea fijo y que los registros estén ordenados con el mismo criterio.

El caso establece que los nombres de grabación son únicos. Si se detecta un nombre duplicado al crear la grabación, se rechaza antes de actualizar el índice; así cada búsqueda identifica una sola grabación.

### SuperCollider y API utilizada

SuperCollider separa `sclang` (lenguaje que define y carga los sintetizadores) de `scsynth` (servidor que crea las instancias y genera el audio). Según `BandClient.java`, se inicia el servidor, se ejecuta `~myband.load` para cargar los SynthDefs y luego Java se comunica directamente con `scsynth`.

El cliente no usa una biblioteca SuperCollider para Java: codifica mensajes OSC (Open Sound Control) y los envía en datagramas UDP. El destino predeterminado es `127.0.0.1:57110`, aunque se pueden indicar host y puerto. El código codifica argumentos enteros, flotantes y cadenas como OSC, incluyendo tags de tipo, terminación nula y relleno a múltiplos de cuatro bytes.

- `/s_new`: crea una instancia con nombre de SynthDef, id de nodo, acción, grupo objetivo y pares nombre/valor de controles. El cliente empieza los ids en 10000, usa `ADD_TO_HEAD` (0) y el grupo 1.
- `/n_set`: cambia controles de un nodo ya creado. Se utiliza para enviar `gate = 0` al apagar notas del órgano.

El cliente envía UDP, pero no espera confirmación ni implementa reintentos; por tanto, un envío exitoso desde Java no demuestra por sí solo que el servidor procesó el mensaje. La aplicación debe tratar explícitamente errores y desconexiones.

### Synths y datos que reciben

`BandClient.java` identifica los SynthDefs `classicPiano`, `stringBass` y `organ70s`. La tabla describe los controles que el cliente envía, no la implementación interna de sonido.

| SynthDef / método Java | Controles enviados | Cantidad | Información conocida |
|---|---|---:|---|
| `classicPiano` / `playPiano` | `note`, `durMs`, `third` | 3 | Acorde sobre una nota MIDI. `third` es 4 para mayor o 3 para menor; si se omite `major`, se usa mayor. |
| `stringBass` / `playBass` | `note`, `durMs` | 2 | Nota de bajo con duración en milisegundos. |
| `organ70s` / `noteOn` | `freq`, `r1`, `r2`, `r3`, `a1`, `a2`, `a3` | 7 | Nota sostenida hasta `noteOff`. La frecuencia se calcula desde la nota MIDI. Razones predeterminadas: 1, 2, 3; amplitudes: 1, 0.5, 0.25. |

El cliente acepta notas MIDI como enteros y el proyecto anota el rango habitual 0–127, pero `BandClient.java` no lo valida. Tampoco establece rangos para duración, razones parciales o amplitudes; esos límites deben definirse en la aplicación o confirmarse contra las definiciones de los SynthDefs. `noteOn(note, ratios, amps)` exige arreglos de exactamente tres razones y tres amplitudes. `noteOff(note)` localiza el nodo asociado y envía `/n_set` con `gate = 0`; `allNotesOff()` apaga todas las notas de órgano activas.

El archivo `myband.sc` se menciona en el caso, pero no está disponible en esta carpeta. No se pueden confirmar los osciladores, envolventes, filtros, timbre, rangos internos ni otros controles sin revisar ese archivo.

### Arquitectura de redes y mensajes

Flujo local propuesto:

`dispositivo → adaptador/procesamiento Java → comando musical → OSC/UDP → scsynth → salida de audio`

- El adaptador de cada dispositivo convierte sus datos (por ejemplo, micrófono, control genérico o tableta) a valores musicales y valida entradas y desconexiones.
- Un comando musical independiente del dispositivo representa acciones como iniciar nota, cambiar controles o detener nota; incluye el synth y los parámetros necesarios.
- El cliente OSC traduce el comando a mensajes como `/s_new` o `/n_set` y los envía al host/puerto del servidor.
- El editor observa los mismos comandos para registrarlos con tiempo relativo, synth y parámetros; al reproducirlos, vuelve a emitirlos en los tiempos guardados. Deben registrarse también las acciones de apagado de notas sostenidas.

Bridge separa la abstracción de los instrumentos de la implementación que invoca synths/comandos. Cualquier tipo de instrumento puede trabajar con una interfaz común de comandos. Si un instrumento tiene una sola referencia a synth, asignar uno nuevo reemplaza al anterior; controlar varios synths desde el mismo dispositivo requiere una relación múltiple explícita o varias instancias/adaptadores.

En una ejecución local, Java puede usar `127.0.0.1`. Un instrumento externo requiere además una ruta de entrada a Java (USB, Bluetooth, red u otra, según el dispositivo). Si se usa una aplicación Android, el enunciado pide que el editor pueda recibir sus mensajes actuando como servidor. El transporte y formato entre ese instrumento externo y Java todavía no están especificados; no se debe asumir que son OSC solo porque Java use OSC para comunicarse con `scsynth`.