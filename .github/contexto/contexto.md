# Contexto:
## Detalles
'+' -> publico
'#' -> protected
No se incluyen metodos privados

Contiene un paquete sus clases y una descripcion breve de que hace,
contiene los metodos con los que se puede interactura desde afuera.

Es una referencia para su uso.

Resumen general: PowerBand captura entradas (microfono, control, tableta), las convierte en comandos musicales, los ejecuta en sintetizadores (SuperCollider via OSC/UDP), y permite grabarlos y reproducirlos desde archivos binarios de registros de tamano fijo (musica, lista e indice). Todo se coordina desde `Controlador` y se maneja por la UI Swing mediante `FachadaControlador`.

## Paquetes

### archivos
Lectura/escritura de archivos binarios de registros de tamano fijo (big endian). Maneja los tres archivos de grabaciones: musica (`recordings.music`), lista (`recordings.list`, ordenada por fecha y nombre) e indice (`recordings.index`, ordenado por nombre, sin duplicados, insensible a mayusculas).

#### `ArchivoRegistros<T>` (final, AutoCloseable)
Acceso generico por offset a un archivo de registros de tamano fijo. Valida offsets (multiplos del tamano), longitud completa y no permite escrituras con hueco.
- `+ ArchivoRegistros(Path ruta, int tamanoRegistro, Function<byte[],T> decodificador, Function<T,byte[]> serializador)`
- `+ long cantidadRegistros() throws IOException`
- `+ T leer(long posicionBytes) throws IOException`
- `+ void escribir(long posicionBytes, T registro) throws IOException` (posicion <= tamano del archivo; escribir en el final agrega)
- `+ void close() throws IOException`

#### `BuscadorIndice` (final)
Busqueda binaria de un nombre en el archivo de indice.
- `+ BuscadorIndice(Path rutaIndice)`
- `+ Optional<EntradaIndice> buscar(String nombre) throws IOException`

#### `ComandoGrabado` (final)
Registro de un comando en disco: tiempo relativo (ms), id de sintetizador, tipo de accion y hasta 8 parametros double. Tamano: `TAMANO_COMANDO_GRABADO`.
- `+ ComandoGrabado(long tiempoRelativoMillis, IdSintetizador idSintetizador, TipoAccion tipoAccion, double[] parametros)`
- `+ long tiempoRelativoMillis()`
- `+ IdSintetizador idSintetizador()`
- `+ TipoAccion tipoAccion()`
- `+ double[] parametros()` (copia)
- `+ byte[] serializar()`
- `+ static ComandoGrabado desdeBytes(byte[] datos)`
- `+ String toString()`

#### `ComparadorNombres` (final, Comparator<String>)
Comparador de nombres sin distinguir mayusculas. Singleton.
- `+ static final ComparadorNombres INSTANCIA`
- `+ int compare(String nombre1, String nombre2)`

#### `EncabezadoGrabacion` (final)
Encabezado que precede los comandos de una grabacion en el archivo de musica: nombre, posicion inicial, posicion final y fecha (epoch millis).
- `+ EncabezadoGrabacion(String nombre, long posicionInicial, long posicionFinal, long fechaEpochMillis)`
- `+ String nombre()`
- `+ long posicionInicial()`
- `+ long posicionFinal()`
- `+ long fechaEpochMillis()`
- `+ byte[] serializar()`
- `+ static EncabezadoGrabacion desdeBytes(byte[] datos)`
- `+ String toString()`

#### `EntradaIndice` (final)
Registro del indice: nombre y rango [posicionInicial, posicionFinal] dentro del archivo de musica.
- `+ EntradaIndice(String nombre, long posicionInicial, long posicionFinal)`
- `+ String nombre()`
- `+ long posicionInicial()`
- `+ long posicionFinal()`
- `+ byte[] serializar()`
- `+ static EntradaIndice desdeBytes(byte[] datos)`
- `+ String toString()`

#### `EntradaListaGrabacion` (final)
Registro de la lista de grabaciones: nombre y fecha.
- `+ EntradaListaGrabacion(String nombre, long fechaEpochMillis)`
- `+ String nombre()`
- `+ long fechaEpochMillis()`
- `+ byte[] serializar()`
- `+ static EntradaListaGrabacion desdeBytes(byte[] datos)`
- `+ String toString()`

#### `ListaGrabaciones` (final)
Administra el archivo de lista. Al agregar usa archivo temporal, valida orden/cantidad y reemplaza de forma atomica. Rechaza nombres duplicados (`NombreDuplicadoException`).
- `+ ListaGrabaciones(Path ruta)`
- `+ void agregar(EntradaListaGrabacion nuevaEntrada) throws IOException`
- `+ List<EntradaListaGrabacion> listar() throws IOException`

#### `OrdenadorIndice` (final)
Ordena el archivo de indice por nombre con merge sort externo (runs distribuidos y mezclados en temporales). Lanza `NombreDuplicadoException` si hay nombres repetidos.
- `+ OrdenadorIndice(Path rutaIndice)`
- `+ Path ordenar() throws IOException` (devuelve la ruta del temporal ya ordenado; quien lo recibe debe reemplazarlo/eliminarlo)
- `+ void reemplazarResultado(Path rutaResultado) throws IOException`
- `+ void ordenarYReemplazar() throws IOException`

#### `ReemplazadorIndice` (final)
Valida (cantidad y orden estricto) y reemplaza el indice con un archivo resultado, de forma atomica.
- `+ ReemplazadorIndice(Path rutaIndice, Path rutaResultado)`
- `+ void reemplazar() throws IOException` (espera la misma cantidad de registros que el indice actual)
- `+ void reemplazar(long cantidadResultadoEsperada) throws IOException`

#### `ValidacionesGenerales` (interface)
Validacion y codificacion de nombres (UTF-8, 1 a 200 bytes, sin bytes nulos).
- `+ static void validarNombre(String nombre)`
- `+ static byte[] codificarNombre(String nombre)`
- `+ static String decodificarNombre(byte[] bytes)`

---

### comandos
Comandos musicales, su ejecucion sobre sintetizadores y el bus que los distribuye a observadores (por ejemplo el `Grabador`).

#### `Comando` (interface)
- `+ void ejecutar()`
- `+ Sintetizador destino()`
- `+ TipoAccion tipoAccion()`
- `+ double[] parametros()`

#### `TipoAccion` (enum)
Valores: `PUNTUAL(1)`, `INICIO(2)`, `ACTUALIZACION(3)`, `FINALIZACION(4)`.
- `+ int codigo()`
- `+ static TipoAccion desdeCodigo(int codigo)`

#### `ObservadorComandos` (interface funcional)
- `+ boolean comandoEnviado(Comando comando, long momentoNanos)` (true si lo proceso)

#### `BusComandos` (final)
Bus de comandos. Notifica a todos los observadores; los fallos se registran en el log sin interrumpir.
- `+ void registrarObservador(ObservadorComandos observador)`
- `+ void quitarObservador(ObservadorComandos observador)`
- `+ void publicar(Comando comando, long momentoNanos)`

#### `ComandoPuntual` (final, Comando)
Accion de un solo momento (nota con duracion, evento de control, posicion de tableta). Al ejecutar registra en el log.
- `+ ComandoPuntual(Sintetizador destino, double... parametros)`
- `+ ComandoPuntual(Sintetizador destino, TipoAccion tipoAccion, double... parametros)` (tipoAccion debe ser PUNTUAL)
- `+ void ejecutar()` / `+ Sintetizador destino()` / `+ TipoAccion tipoAccion()` / `+ double[] parametros()` / `+ String toString()`

#### `ComandoSostenido` (abstract, Comando)
Base de comandos con identificador de accion (inicio/actualizacion/finalizacion). `ejecutar`, `destino`, `tipoAccion` y `parametros` son final.
- `# ComandoSostenido(Sintetizador destino, TipoAccion tipoAccion, String identificadorAccion, double... parametros)`
- `+ final void ejecutar()` / `+ final Sintetizador destino()` / `+ final TipoAccion tipoAccion()` / `+ final double[] parametros()`
- `+ String identificadorAccion()`
- `+ String identificador()`
- `# String nombreTipo()`
- `+ String toString()`

#### `ComandoInicio` / `ComandoActualizacion` / `ComandoFinalizacion` (final, extienden ComandoSostenido)
Comandos de tipo INICIO, ACTUALIZACION y FINALIZACION. Constructores (mismo patron en los tres, aqui `X` es la clase):
- `+ X(Sintetizador destino, String identificadorAccion, double... parametros)`
- `+ X(Sintetizador destino, double... parametros)` (identificador por defecto: "inicio" / "actualizacion" / "finalizacion")
- `+ X(Sintetizador destino, long identificadorAccion, double... parametros)`
- `+ X(Sintetizador destino, TipoAccion tipoAccion, String identificadorAccion, double... parametros)` (valida que tipoAccion corresponda)

#### `ComandoReconstruido` (final, Comando)
Comando reconstruido desde un registro grabado o creado por un instrumento. Ejecuta sin registrar en el log.
- `+ ComandoReconstruido(Sintetizador destino, TipoAccion tipoAccion, double[] parametros)`
- `+ void ejecutar()` / `+ Sintetizador destino()` / `+ TipoAccion tipoAccion()` / `+ double[] parametros()`

#### `FabricaComandosGrabados` (final)
Convierte `ComandoGrabado` en `Comando` ejecutable resolviendo el sintetizador por id.
- `+ FabricaComandosGrabados(Function<IdSintetizador,Sintetizador> resolver)`
- `+ Comando reconstruir(ComandoGrabado grabado)`

---

### controlador
Coordinacion central: conexion, instrumentos, sintetizadores, grabacion, reproduccion y configuracion.

#### `EstadoControlador` (enum)
`INICIAL, CONECTANDO, CONECTADO, GRABANDO, REPRODUCIENDO, ERROR`

#### `ObservadorEstado` (interface)
- `+ void estadoCambiado(EstadoControlador estado)`
- `+ void errorNotificado(String mensaje, Throwable causa)`

#### `FachadaControlador` (interface)
Contrato que usa la UI.
- `+ boolean conectar()`
- `+ void desconectar()`
- `+ List<String> listarDispositivos()`
- `+ boolean asignarDispositivo(String instrumentoId, String dispositivoId)`
- `+ boolean asignarSintetizador(String instrumentoId, IdSintetizador sintetizadorId)`
- `+ List<String> listarInstrumentos()`
- `+ List<IdSintetizador> listarSintetizadores()`
- `+ Map<String,String> dispositivoPorInstrumento()`
- `+ Map<String,List<IdSintetizador>> sintetizadoresPorInstrumento()`
- `+ boolean reemplazarSintetizadores(String instrumentoId, List<IdSintetizador> sintetizadorIds)`
- `+ Map<String,Object> opcionesConfigurables()`
- `+ Object consultarOpcion(String clave)`
- `+ void establecerOpcion(String clave, Object valor)`
- `+ void iniciarGrabacion()`
- `+ void detenerGrabacion(String nombre)`
- `+ List<String> listarSesiones()`
- `+ List<String> buscarSesiones(String nombre)`
- `+ boolean reproducirSesion(String nombre)`
- `+ void registrarObservadorEstado(ObservadorEstado observador)`
- `+ void retirarObservadorEstado(ObservadorEstado observador)`

#### `Controlador` (final, Singleton, implementa FachadaControlador)
Implementacion real. Instrumentos validos: "microfono", "control", "tableta". Crea `BandClient` y los 3 sintetizadores al conectar; revierte fallos locales de apertura o activacion; la reproduccion corre en un hilo daemon; notifica estados y errores a observadores; una desconexion individual informa el fallo y no debe detener las demas entradas, mientras que un fallo global vuelve a INICIAL. Al desconectar, detiene las entradas, aborta y limpia grabaciones incompletas, espera el hilo de reproduccion, libera notas y cierra el cliente.
- `+ static Controlador getInstance()`
- `+ static Controlador obtenerInstancia()`
- `+ EstadoControlador estadoActual()`
- Implementa todos los metodos de `FachadaControlador`.
  - Las asignaciones validan instrumentos y dispositivos detectados, evitan asignar un dispositivo a dos instrumentos y actualizan la entrada de control en ejecucion.
  - Las asignaciones de sintetizadores se reflejan en los instrumentos, aceptan conjuntos multiples y se conservan para resolver los destinos del reproductor.
  - El grabador se registra en `BusComandos` solo durante una grabacion; el reproductor usa la misma tabla de sintetizadores del controlador.
  - `iniciarGrabacion()` lanza `IllegalStateException` si no esta CONECTADO o ya graba/reproduce.
  - `detenerGrabacion(nombre)` finaliza y guarda la grabacion (lanza si no hay grabacion activa o el nombre es duplicado/invalido).
  - `buscarSesiones(nombre)` devuelve las sesiones cuyo nombre contiene el texto (sin distinguir mayusculas).
  - `reproducirSesion(nombre)` devuelve false si el estado no es valido o la sesion no existe.
  - Los fallos locales de transporte se identifican con `FalloTransporteException`; UDP no confirma la disponibilidad o ejecucion remota de `scsynth`.

#### `FachadaControladorFalsa` (final, implementa FachadaControlador)
Implementacion de desarrollo con datos de ejemplo en memoria (dispositivos "audio-demo", "gamepad-demo", "lapiz-demo"; instrumentos "piano", "bajo", "organo"; sesiones "Demo Piano", "Demo Organo"). Sin hardware ni archivos. No es la fachada usada por el arranque normal de `Main`.
- `+ FachadaControladorFalsa()` (constructor por defecto)
- Implementa todos los metodos de `FachadaControlador`.

---

### excepciones
Excepciones con codigo de error y mensaje formateado (`CODIGO mensaje [ruta]`).

#### `CodigoError` (interface)
- `+ String codigo()`
- `+ String plantilla()`
- `+ String formatear(Object... argumentos)`

#### `FalloTransporteException` (final, extends IllegalStateException)
Identifica un error local de transporte y conserva la causa original. No confirma un fallo remoto.
- `+ FalloTransporteException(String mensaje, Throwable causa)`

#### `ConstantesExcepciones` (final)
Prefijos (`ARC-`, `ORD-`, `ARG-`, `EST-`) y plantillas de mensajes (constantes `MSG_*`). Solo constantes publicas.

#### `ArchivoException` (extends IOException, CodigoError)
Errores de archivo.
- `+ ArchivoException(TipoErrorArchivo tipo, String ruta)`
- `+ ArchivoException(TipoErrorArchivo tipo, String ruta, Throwable causa)`
- `+ ArchivoException(TipoErrorArchivo tipo, String ruta, long posicion)`
- `+ ArchivoException(TipoErrorArchivo tipo, String ruta, long posicion, long tamano)`
- `+ ArchivoException(TipoErrorArchivo tipo, String ruta, String valor)`
- `# ArchivoException(CodigoError tipo, String ruta, Object... argumentos)`
- `# ArchivoException(CodigoError tipo, String ruta, Throwable causa, Object... argumentos)`
- Fabricas: `+ static archivoCerrado(String ruta)`, `+ static registroFueraDeRango(String ruta, long posicion)`, `+ static escrituraConHueco(String ruta, long posicion, long tamano)`, `+ static reemplazoNoAtomico(String ruta)`, `+ static rutaNoRegular(String ruta)`, `+ static registroCorrupto(String ruta, Throwable causa)`, `+ static errorES(String ruta, String operacion, Throwable causa)`
- `+ final CodigoError tipo()` / `+ final TipoErrorArchivo tipoArchivo()` / `+ final String ruta()` / `+ final String codigo()` / `+ final String plantilla()`

#### `ArchivoIncompletoException` (final, extends ArchivoException)
El tamano del archivo no es multiplo del registro.
- `+ ArchivoIncompletoException(String ruta, long tamano, long tamanoRegistro)`
- `+ ArchivoIncompletoException(long tamano, long tamanoRegistro, String ruta)`
- `+ long tamano()` / `+ long tamanoRegistro()` / `+ long residuo()`

#### `OrdenamientoException` (extends ArchivoException)
Errores de orden/duplicados/cantidad del indice o lista.
- `+ OrdenamientoException(TipoErrorOrdenamiento tipo, String ruta, String nombre)`
- `+ OrdenamientoException(TipoErrorOrdenamiento tipo, String ruta, String nombre1, String nombre2)`
- `+ OrdenamientoException(TipoErrorOrdenamiento tipo, String ruta, long esperada, long obtenida)`
- `+ OrdenamientoException(TipoErrorOrdenamiento tipo, String ruta, Throwable causa, Object... argumentos)`
- `+ TipoErrorOrdenamiento tipoOrdenamiento()`
- `+ static OrdenamientoException indiceDesordenado(String ruta, String nombre)`

#### `NombreDuplicadoException` (final, extends OrdenamientoException)
- `+ NombreDuplicadoException(String nombre1, String nombre2)`
- `+ NombreDuplicadoException(String nombre1, String nombre2, String ruta)`
- `+ String nombre1()` / `+ String nombre2()`

#### `CantidadInesperadaException` (final, extends OrdenamientoException)
- `+ CantidadInesperadaException(String ruta, long esperada, long obtenida)`
- `+ CantidadInesperadaException(long esperada, long obtenida, String ruta)`
- `+ long esperada()` / `+ long obtenida()`

#### `ArgumentoInvalidoException` (extends IllegalArgumentException, CodigoError)
Argumentos invalidos (nombres, offsets, intervalos, parametros).
- `+ ArgumentoInvalidoException()`
- `+ ArgumentoInvalidoException(TipoErrorArgumento tipo, String detalle)`
- Fabricas estaticas: `nombreInvalido()`, `nombreInvalido(Throwable)`, `nombreNulo()`, `nombreConNulos()`, `nombreUtf8Invalido(Throwable)`, `rellenoNombreInvalido()`, `nombreVacio()`, `nombreCodificadoInvalido(Throwable)`, `offsetInvalido(int tamanoRegistro, long posicion)`, `intervaloInvalido(long inicio, long fin)`, `tamanoRegistroInvalido()`, `detalle(TipoErrorArgumento, String)`, `tiempoRelativoNegativo()`, `parametrosNoUtilizados()`, `parametrosExceso(int maximo, int cantidad)`, `comandoNulo()`, `momentoAnterior()`
- `+ final TipoErrorArgumento tipo()` / `+ final String codigo()` / `+ final String plantilla()`

#### `EstadoInvalidoException` (extends IllegalStateException, CodigoError)
- `+ EstadoInvalidoException(TipoErrorEstado tipo)`
- `+ EstadoInvalidoException(TipoErrorEstado tipo, Throwable causa)`
- `+ static temporalAjeno()` / `+ static grabacionActiva()` / `+ static grabacionInactiva()`
- `+ final TipoErrorEstado tipo()` / `+ final String codigo()` / `+ final String plantilla()`

### excepciones.tipos
Enums con codigo y plantilla (implementan `CodigoError`: `+ String codigo()`, `+ String plantilla()`).
- `TipoErrorArchivo`: `ARCHIVO_CERRADO, ARCHIVO_INCOMPLETO, REGISTRO_FUERA_DE_RANGO, REGISTRO_CORRUPTO, ESCRITURA_CON_HUECO, REEMPLAZO_NO_ATOMICO, RUTA_NO_REGULAR, ERROR_ES`
- `TipoErrorArgumento`: `NOMBRE_INVALIDO, OFFSET_INVALIDO, INTERVALO_INVALIDO, TAMANO_REGISTRO_INVALIDO, PARAMETROS_EXCESO`
- `TipoErrorEstado`: `TEMPORAL_AJENO, GRABACION_ACTIVA, GRABACION_INACTIVA`
- `TipoErrorOrdenamiento`: `NOMBRE_DUPLICADO, INDICE_DESORDENADO, CANTIDAD_INESPERADA, LIMPIEZA_FALLIDA`

---

### grabaciones
Ciclo de vida y persistencia de grabaciones: grabar (archivo temporal), finalizar (agregar a musica/lista/indice con respaldo y rollback), cargar y reproducir.

#### `ArchivosGrabacion` (final)
Rutas de los tres archivos; los crea si no existen.
- `+ static ArchivosGrabacion inicializar() throws IOException` (usa `Constantes.RUTA_GRABACIONES`)
- `+ static ArchivosGrabacion inicializar(Path directorio) throws IOException`
- `+ Path directorio()` / `+ Path archivoMusica()` / `+ Path archivoLista()` / `+ Path archivoIndice()`

#### `Grabador` (final, ObservadorComandos)
Graba los comandos publicados en el bus a un archivo temporal de sesion (tiempo relativo en ms desde el inicio).
- `+ Grabador(Path directorioGrabaciones)`
- `+ Grabador(Path directorioGrabaciones, LongSupplier relojMonotonico)`
- `+ void iniciar() throws IOException`
- `+ void detener() throws IOException`
- `+ boolean estaGrabando()`
- `+ Path archivoSesionTemporal()`
- `+ long cantidadComandos() throws IOException`
- `+ boolean comandoEnviado(Comando comando, long momentoNanos)`

#### `FinalizadorGrabacion` (final)
Guarda la sesion grabada con un nombre: valida, agrega encabezado y comandos a musica, entrada a lista e indice, reordena el indice y reemplaza todo con respaldo (restaura si falla).
- `+ FinalizadorGrabacion(Grabador grabador, ArchivosGrabacion archivos)`
- `+ FinalizadorGrabacion(Grabador grabador, ArchivosGrabacion archivos, LongSupplier relojFecha)`
- `+ void finalizar(String nombre) throws IOException` (lanza `NombreDuplicadoException` si el nombre existe)

#### `LimpiezaGrabacion` (final)
Elimina el temporal de la sesion.
- `+ LimpiezaGrabacion(Path archivoTemporal, ArchivosGrabacion archivos)`
- `+ void limpiar(Throwable fallo)` (limpia y registra el error)
- `+ void limpiarExitosamente() throws IOException`

#### `CargadorGrabaciones` (final)
Lista y abre grabaciones guardadas (busca en el indice y valida el encabezado).
- `+ CargadorGrabaciones(ArchivosGrabacion archivos)`
- `+ List<EntradaListaGrabacion> listar() throws IOException`
- `+ Optional<GrabacionAbierta> abrir(String nombre) throws IOException`

#### `GrabacionAbierta` (final, AutoCloseable)
Lectura secuencial de los comandos de una grabacion abierta.
- `+ EncabezadoGrabacion encabezado()`
- `+ Optional<ComandoGrabado> siguienteComando() throws IOException` (vacio al terminar)
- `+ void close() throws IOException`
- `+ static byte[] leerBytes(FileChannel canal, Path ruta, long posicion, int cantidad) throws IOException`

#### `EsperaReproduccion` (interface funcional)
- `+ void esperar(long nanosegundos) throws InterruptedException`

#### `Reproductor` (final)
Reproduce una grabacion respetando los tiempos (buffer de 32 comandos). Al terminar o detenerse finaliza las notas sostenidas activas.
- `+ Reproductor(Function<ComandoGrabado,Comando> reconstruir, LongSupplier relojMonotonico, EsperaReproduccion espera)`
- `+ void reproducir(GrabacionAbierta grabacion) throws IOException, InterruptedException` (bloquea; cierra la grabacion)
- `+ boolean detener()` (true si habia reproduccion en curso)

---

### input
Captura de datos desde dispositivos en hilos propios y entrega a los instrumentos. Valida rangos (`IllegalArgumentException` si el valor es invalido).

#### `Entrada` (abstract, AutoCloseable)
Base de una entrada con hilo daemon y notificacion de desconexion.
- `# Entrada(String nombre)` / `# Entrada(String nombre, Consumer<Entrada> callbackDesconexion)` / `# Entrada(String nombre, Runnable callbackRunnable)`
- `+ String nombre()`
- `+ void iniciar()` / `+ void detener()` / `+ boolean estaActiva()` (alias: `activa()`, `estaIniciada()`, `iniciarHilo()`, `detenerHilo()`)
- `detener()` interrumpe y espera el hilo de captura antes de completar la liberacion local.
- `+ void cambiarCallbackDesconexion(Consumer<Entrada>)` / `+ void registrarCallbackDesconexion(Consumer<Entrada>)` / `+ void registrarCallbackDesconexion(Runnable)`
- `+ void registrarObservadorDesconexion(ObservadorDesconexion observador)`
- `+ void desconectar(String motivo)` / `+ void desconectar(String motivo, Throwable causa)` (alias: `notificarDesconexion(String)`)
- `+ void rechazarValor(String descripcion, double valor, double minimo, double maximo)` (registra y lanza excepcion)
- `+ void liberarRecursos()`
- `+ void close()`
- `# void validarValor(String descripcion, double valor, double minimo, double maximo)`
- `# abstract void ejecutarCaptura() throws Exception`
- `# void liberarRecursosEspecificos()`

#### `ObservadorDesconexion` (interface funcional)
- `+ void entradaDesconectada(Entrada entrada, String motivo, Throwable causa)`

#### `EntradaMicrofono` (extends Entrada)
Captura el microfono (TarsosDSP, FFT_YIN) y convierte tono a nota via `Microfono`. Rangos: frecuencia 20 a 20000 Hz, amplitud 0 a 1.
- `+ EntradaMicrofono()` / `+ EntradaMicrofono(Microfono)` / `+ EntradaMicrofono(Microfono, Consumer<Entrada>)` / `+ EntradaMicrofono(Microfono, Runnable)` / `+ EntradaMicrofono(Microfono, BiConsumer<Double,Double> receptor, Consumer<Entrada>)`
- `+ void procesarFrecuencia(double frecuenciaHz, double amplitud)`
- `# void ejecutarCaptura() throws Exception` / `# void liberarRecursosEspecificos()`

#### `EntradaControl` (extends Entrada)
Captura un gamepad/control (JInput) y lo envia a `Control`. Excluye teclado y mouse.
- `+ EntradaControl()` / `+ EntradaControl(Control)` / `+ EntradaControl(Control, String nombreDispositivo)` / `+ EntradaControl(Control, String nombreDispositivo, Consumer<Entrada>)`
- `+ static List<String> detectarDispositivos()` (alias: `listarDispositivos()`, `detectarControladores()`)
- `+ static boolean esDispositivoValido(String nombre)` / `+ static boolean esDispositivoValido(Controller controlador)` (alias: `excluirTecladoOMouse(String)`)
- `+ void seleccionarDispositivo(String nombre)`
- `+ void limpiarDispositivoSeleccionado()`
- `+ void procesarEvento(double eje, double valor)` (valores en [0,1])
- `# void ejecutarCaptura() throws Exception` / `# void liberarRecursosEspecificos()`

#### `EntradaTableta` (extends Entrada)
Captura una tableta grafica (JPen por reflexion) y envia a `Tableta` posicion normalizada (0 a 1) y presion.
- `+ EntradaTableta()` / `+ EntradaTableta(Tableta)` / `+ EntradaTableta(Tableta, Consumer<Entrada>)`
- `+ double[] normalizarPosicion(double posicionX, double posicionY, double presion)`
- `+ void procesarPosicion(double posicionX, double posicionY, double presion)`
- `+ static Rectangle areaCompletaPantallas()`
- `# void ejecutarCaptura() throws Exception` / `# void liberarRecursosEspecificos()`

#### `NormalizadorEntrada` (final)
Normalizacion lineal de rangos (lanza `IllegalArgumentException` si el valor esta fuera del rango origen).
- `+ static double normalizar(double valor, double origenMin, double origenMax, double destinoMin, double destinoMax)`
- `+ static double normalizarCeroUno(double valor, double origenMin, double origenMax)`
- `+ static boolean estaEnRango(double valor, double minimo, double maximo)`
- `+ double normalizarValor(double valor, double origenMin, double origenMax, double destinoMin, double destinoMax)`

---

### instrumentos
Instrumentos que convierten datos de entrada en comandos, los ejecutan en sus sintetizadores y los publican en el `BusComandos`.

#### `Instrumento` (abstract)
Mantiene uno o varios sintetizadores destino. `enviarComando` ejecuta y publica en el bus (con `System.nanoTime()`).
- `# Instrumento()` / `# Instrumento(Sintetizador)` / `# Instrumento(BusComandos)` / `# Instrumento(Sintetizador, BusComandos)` / `# Instrumento(List<? extends Sintetizador>, BusComandos)`
- `+ Sintetizador sintetizador()` (alias: `obtenerSintetizador()`)
- `+ Sintetizador reemplazarSintetizador(Sintetizador nuevo)` (devuelve el anterior; alias: `asignarSintetizador`, `cambiarSintetizador`)
- `+ void agregarSintetizador(Sintetizador nuevo)`
- `+ void retirarSintetizador(Sintetizador retirar)`
- `+ void reemplazarSintetizadores(List<? extends Sintetizador> nuevos)`
- `+ List<Sintetizador> sintetizadores()`
- `+ BusComandos busComandos()`
- `+ void enviarAccion(TipoAccion tipoAccion, double... parametros)` (alias: `ejecutarAccion`; a todos los sintetizadores)
- `+ void enviarComando(Comando comando)`
- `# void publicarComando(Comando comando)`
- `# void registrarError(String mensaje, RuntimeException excepcion)`

#### `Microfono` (extends Instrumento)
Convierte frecuencia en nota MIDI (A4 = 440 Hz) y envia `ComandoPuntual(notaMidi, amplitud)`.
- `+ Microfono()` / `+ Microfono(Sintetizador)` / `+ Microfono(Sintetizador, BusComandos)`
- `+ Comando convertirFrecuencia(double frecuenciaHz, double amplitud)` (alias: `procesarFrecuencia`)
- `+ double frecuenciaAMidi(double frecuenciaHz)`

#### `Control` (extends Instrumento)
Envia `ComandoPuntual(eje, valor)`.
- `+ Control()` / `+ Control(Sintetizador)` / `+ Control(Sintetizador, BusComandos)`
- `+ Comando convertirEvento(double eje, double valor)` (alias: `procesarEvento`)

#### `Tableta` (extends Instrumento)
Envia `ComandoPuntual(x, y, presion)`.
- `+ Tableta()` / `+ Tableta(Sintetizador)` / `+ Tableta(Sintetizador, BusComandos)`
- `+ Comando convertirPosicion(double posicionX, double posicionY, double presion)` (alias: `procesarPosicion`)

---

### logging
Registro centralizado en `powerband/logs/powerband.log` (formato: `instante [categoria] mensaje ubicacion=...`).

#### `CategoriaLog` (enum)
`COMANDO, INFO, ERROR`
- `+ String texto()`

#### `LoggerPowerBand` (final, Singleton)
- `+ static LoggerPowerBand getInstance()`
- `+ static void cambiarPath(Path path)` (cambia la carpeta de logs)
- `+ void registrarComando(Object objeto)`
- `+ void registrarInfo(Object objeto)`
- `+ void registrarError(Object objeto)`
- `+ void registrarErrorArchivo(Object objeto, String ubicacion)`
- `+ void registrar(CategoriaLog categoria, Object objeto)`
- Lanza `IllegalStateException` si no puede escribir el log.

---

### main
#### `Main`
Punto de entrada. Abre `VentanaPrincipal` en el EDT y le inyecta la instancia singleton real `Controlador.getInstance()`.
- `+ static void main(String[] args)`

---

### sintetizadores
Sintetizadores de SuperCollider (`myband.sc`: classicPiano, stringBass, organ70s) controlados por OSC/UDP.

#### `Sintetizador` (interface)
- `+ IdSintetizador id()`
- `+ void ejecutar(TipoAccion tipoAccion, double... parametros)` (lanza `IllegalArgumentException` si accion/parametros no son validos)
- `+ void liberarTodasLasNotas()`

#### `IdSintetizador` (enum)
`PIANO(1)`, `BAJO(2)`, `ORGANO(3)`
- `+ int codigo()`
- `+ static IdSintetizador desdeCodigo(int codigo)`

#### `BandClient` (AutoCloseable)
Cliente OSC directo a scsynth (`/s_new`, `/n_set`). Destino por defecto desde `Configuraciones`.
- `+ BandClient() throws IOException` / `+ BandClient(Configuraciones) throws IOException` / `+ BandClient(String host, int port) throws IOException`
- `+ void playPiano(int note, int durMs) throws IOException` (acorde mayor)
- `+ void playPiano(int note, int durMs, boolean major) throws IOException`
- `+ void playBass(int note, int durMs) throws IOException`
- `+ void noteOn(int note) throws IOException` (parciales por defecto)
- `+ synchronized void noteOn(int note, float[] ratios, float[] amps) throws IOException` (3 razones y 3 amplitudes)
- `+ synchronized void noteOff(int note) throws IOException`
- `+ synchronized void allNotesOff() throws IOException`
- `+ synchronized void close() throws IOException`
- `+ static void main(String[] args) throws Exception` (prueba manual)

#### `PianoSintetizador` (final)
Solo `PUNTUAL`. Parametros: `[nota MIDI, duracion ms, (tercera: 3 menor | 4 mayor)]` (2 o 3 parametros).
- `+ PianoSintetizador(BandClient)` / `+ id()` / `+ ejecutar(TipoAccion, double...)` / `+ liberarTodasLasNotas()`

#### `BajoSintetizador` (final)
Solo `PUNTUAL`. Parametros: `[nota MIDI, duracion ms]`.
- `+ BajoSintetizador(BandClient)` / `+ id()` / `+ ejecutar(TipoAccion, double...)` / `+ liberarTodasLasNotas()`

#### `OrganoSintetizador` (final)
Solo `INICIO` y `FINALIZACION`. INICIO: `[nota]` o `[nota, r1, r2, r3, a1, a2, a3]`. FINALIZACION: `[nota]`.
- `+ OrganoSintetizador(BandClient)` / `+ id()` / `+ synchronized ejecutar(TipoAccion, double...)` / `+ synchronized liberarTodasLasNotas()`

(`AdaptadorBandClient` es package-private: base con validaciones comunes.)

---

### ui
Interfaz grafica Swing. Todo accede al sistema solo mediante `FachadaControlador`; las operaciones largas usan `SwingWorker`.

#### `Vista` (enum)
`INICIO, CONEXION, INSTRUMENTOS, CONFIGURACION, GRABACION, SESIONES`
- `+ String etiqueta()` / `+ String descripcion()` / `+ String tareaResponsable()`

#### `ConstantesGUI` (final)
Colores, tamanos, margenes y espaciados de la interfaz (solo constantes `public static final`).

#### `VentanaPrincipal` (final, extends JFrame)
Ventana principal ("PowerBand"). Al cerrar desconecta el controlador en segundo plano antes de `dispose()`.
- `+ VentanaPrincipal(FachadaControlador controlador)`
- `# VentanaPrincipal(FachadaControlador controlador, Runnable manejadorCierreCompletado)`
- `+ FachadaControlador controlador()`
- `+ void establecerAccionCierre(Runnable accionCierre)`
- `+ final void cerrar()`
- `+ final void limpiarObservadorEstado()`
- `# boolean cierreCompletado()`

#### `PanelPrincipal` (final, extends JPanel)
Navegacion lateral, area de contenido (CardLayout) y barra de estado.
- `+ PanelPrincipal(FachadaControlador controlador)`
- `+ final void limpiarEstado()`
- `# FachadaControlador fachada()` / `# Vista vistaActual()` / `# JPanel panelVista(Vista)` / `# JButton botonNavegacion(Vista)` / `# int cantidadVistas()` / `# BarraEstado barraEstado()`

#### `BarraEstado` (final, extends JPanel, ObservadorEstado)
Muestra el estado del controlador y el ultimo error.
- `+ BarraEstado(FachadaControlador controlador)`
- `+ final void limpiar()` (retira el observador)
- `+ final void estadoCambiado(EstadoControlador estado)`
- `+ final void errorNotificado(String mensaje, Throwable causa)`
- `# JLabel etiquetaEstado()` / `# JLabel etiquetaError()` / `# boolean registrada()`

#### `PanelConexion` (final, extends JPanel)
Botones Conectar / Desconectar.
- `+ PanelConexion(FachadaControlador controlador)`

#### `PanelConfiguracion` (final, extends JPanel)
Lista editable de opciones (texto, numeros, booleanos) con boton Guardar por opcion.
- `+ PanelConfiguracion(FachadaControlador controlador)`
- `# PanelConfiguracion(FachadaControlador, Consumer<Throwable> manejadorErrores)`
- `# FachadaControlador fachada()` / `# Component editorOpcion(String clave)` / `# JButton botonGuardar(String clave)` / `# JLabel mensajeVacio()`

#### `PanelGrabacion` (final, extends JPanel)
Iniciar grabacion y detenerla pidiendo un nombre (reintenta si esta duplicado).
- `+ PanelGrabacion(FachadaControlador controlador)`
- `# PanelGrabacion(FachadaControlador, Supplier<String> solicitadorNombre, Consumer<String> avisadorDuplicado, Consumer<Throwable> manejadorErrores)`
- `# FachadaControlador fachada()` / `# JButton botonIniciar()` / `# JButton botonDetener()`

#### `PanelInstrumentos` (final, extends JPanel)
Asignacion de dispositivo y sintetizadores a cada instrumento.
- `+ PanelInstrumentos(FachadaControlador controlador)`
- `# PanelInstrumentos(FachadaControlador, Consumer<Throwable> manejadorErrores)`
- `# FachadaControlador fachada()` / `# JList<String> listaInstrumentos()` / `# JComboBox<String> selectorDispositivo()` / `# JList<IdSintetizador> listaSintetizadores()` / `# JButton botonDispositivo()` / `# JButton botonSintetizadores()` / `# JLabel mensajeEstado()`

#### `PanelSesiones` (final, extends JPanel)
Lista, busqueda y reproduccion de sesiones guardadas.
- `+ PanelSesiones(FachadaControlador controlador)`
- `# PanelSesiones(FachadaControlador, Consumer<Throwable> manejadorErrores)`
- `# FachadaControlador fachada()` / `# JList<String> listaSesiones()` / `# JTextField campoBusqueda()` / `# JButton botonBuscar()` / `# JButton botonMostrarTodas()` / `# JButton botonActualizar()` / `# JButton botonReproducir()` / `# JLabel mensajeEstado()`

---

### util
Configuracion global y constantes.

#### `Configuraciones` (final, Singleton)
Host/puerto del servidor de sonido (por defecto 127.0.0.1:57117) y opciones modificables clave/valor.
- `+ Configuraciones()`
- `+ static Configuraciones getInstance()` (alias: `obtenerInstancia()`)
- `+ String getHost()` / `+ void setHost(String host)`
- `+ int getPuerto()` / `+ void setPuerto(int puerto)` (1 a 65535)
- `+ Object getOpcion(String clave)`
- `+ void setOpcion(String clave, Object valor)`
- `+ void eliminarOpcion(String clave)`
- `+ Map<String,Object> getOpcionesModificables()` (copia inmodificable)

#### `Constantes` (final)
Solo constantes `public static final`:
- Bytes: `ORDEN_BYTES_PROGRAMA` (BIG_ENDIAN).
- Red: `HOST_DEFECTO`, `PUERTO_DEFECTO`, `PUERTO_UDP`.
- Rutas: `RUTA_LOGS` ("powerband/logs"), `RUTA_GRABACIONES` ("powerband/recordings"), `NOMBRE_ARCHIVO_GRABACIONES_MUSICA/LISTA/INDICE`.
- Log: `CATEGORIA_COMANDO/INFO/ERROR`.
- Musica: `NOTA_MIDI_MINIMA/MAXIMA` (0 a 127), `DURACION_MINIMA_MS/MAXIMA_MS`, `TERCERA_MENOR/MAYOR_SEMITONOS` (3/4), parciales del organo (`CANTIDAD_PARCIALES_ORGANO`, `RAZON_ORGANO_1..3`, `AMPLITUD_ORGANO_1..3`), `AMPLITUD_MINIMA/MAXIMA`, `FRECUENCIA_AFINACION_HZ` (440), `SEMITONOS_POR_OCTAVA` (12), `VALOR_MUSICAL_MINIMO/MAXIMO`, `FRECUENCIA_MINIMA/MAXIMA_HZ`, `PRESION_MINIMA/MAXIMA`, `TAMANO_BUFFER_AUDIO`, `SUPERPOSICION_AUDIO`.
- `SINTETIZADORES_INDICES` = {1,2,3} (piano, bajo, organo).
- Formato de archivos: `LONGITUD_MAXIMA_NOMBRE` (200), `MAXIMO_PARAMETROS_COMANDO` (8), `CAPACIDAD_BUFFER_REPRODUCCION` (32), `TAMANO_ENCABEZADO_GRABACION` (224), `TAMANO_COMANDO_GRABADO` (75), `TAMANO_ENTRADA_INDICE` (216), `TAMANO_ENTRADA_LISTA_GRABACION` (208), mas los tamanos de cada campo.