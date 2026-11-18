---
applyTo: "src/main/java/**
---
+ Todos los valores constantes deben estar en util.Constantes
+ La UI no debe de tener ninguna funcionalidad del codigo 
+ Las constantes se escriben NOMBRE_CONSTANTE

# Convenciones de Java para PowerBand

## Plataforma y estructura

- Usa Java 21. El proyecto Maven declara `maven.compiler.release` en 21; no
  introduzcas APIs o sintaxis posteriores.
- Coloca el código de producción bajo `powerband/src/main/java/powerband/` y
  las pruebas correspondientes bajo `powerband/src/test/java/powerband/`,
  reflejando el paquete de producción.
- Usa paquetes en minúsculas, sin espacios ni acentos, y conserva las
  responsabilidades de `input`, `instrumentos`, `comandos`, `sintetizadores`,
  `archivos`, `logging`, `util`, `controlador`, `ui` y `main`.
- Mantén una clase o interfaz pública de nivel superior por archivo, con el
  nombre del archivo igual al del tipo.

## Nombres y formato

- Usa PascalCase para clases, interfaces y enums; camelCase para métodos y
  atributos; y MAYUSCULAS_CON_GUION_BAJO para constantes.
- Conserva los nombres públicos existentes. Para los nuevos conceptos del
  dominio, usa nombres en español y consistentes con los contratos existentes;
  conserva los nombres propios de bibliotecas y protocolos.
- Usa cuatro espacios de indentación, llaves en la misma línea y una línea en
  blanco entre bloques lógicos.
- Ordena imports por biblioteca estándar, JUnit y paquetes del proyecto;
  elimina imports que no se utilicen.
- Añade comentarios o Javadoc solo cuando aclaren una decisión, contrato o
  restricción que no resulte evidente en el código.

## Diseño y responsabilidades

- Prefiere objetos inmutables y campos `final` cuando el estado no necesite
  cambiar. Si expones arreglos o colecciones mutables, devuelve copias o vistas
  no modificables.
- Usa interfaces para contratos compartidos y depende de abstracciones, no de
  implementaciones concretas, cuando el diseño del proyecto lo requiera.
- No mezcles captura de dispositivos, lógica musical, transporte, persistencia
  y presentación en una misma clase.
- La UI presenta información y delega operaciones al controlador; no implementa
  reglas de negocio, lectura directa de dispositivos, OSC ni algoritmos de
  archivos.
- Centraliza los valores constantes compartidos en `util.Constantes`. No
  dupliques valores de configuración o formato ni introduzcas valores mágicos. // Valores cuyo significado no es evidente en el contexto del código.
- Usa `Configuraciones` para opciones modificables y configuración de conexión;
  no dupliques host o puerto en los clientes.
- Usa `LoggerPowerBand` para los eventos y errores definidos por el proyecto.

## Validación y errores

- Valida argumentos y estados en el punto donde se reciben; rechaza valores
  inválidos con una excepción significativa.
- No ocultes errores con bloques `catch` vacíos ni uses defaults que aparenten
  éxito. Captura el tipo de error esperado, añade contexto y vuelve a propagarlo
  o regístralo conforme al contrato.
- Evita `catch (Exception)` general salvo que sea una frontera explícita de la
  aplicación y el error se registre y gestione allí.
- Cierra streams, sockets y otros recursos con `try-with-resources` cuando su
  ciclo de vida lo permita.
- No presentes un envío UDP como confirmación de recepción o ejecución remota.

## Pruebas

- Escribe pruebas con JUnit 5 en `src/test/java`, con nombre de clase
  `NombreDeLaClaseTest`.
- Nombra los métodos de prueba describiendo el comportamiento esperado, por
  ejemplo `rechazaPuertoFueraDeRango`.
- Usa aserciones de JUnit (`assertEquals`, `assertThrows`, `assertTrue`, entre
  otras) y verifica resultados observables, errores y límites relevantes.
- Mantén las pruebas deterministas: no dependas de micrófonos, controles,
  tabletas, SuperCollider, red externa ni esperas con `Thread.sleep`.
- Usa fakes o stubs de interfaces para aislar sintetizadores, dispositivos,
  relojes y observadores. Usa `@TempDir` para pruebas de archivos y logs.
- Evita compartir estado mutable entre pruebas; inicializa o restaura
  singletons, configuración y rutas temporales cuando corresponda.
- Separa las verificaciones que requieren hardware o SuperCollider como pruebas
  manuales de integración; no hagas que `mvn test` dependa de esos recursos.
- Ejecuta las pruebas Maven antes de integrar cambios y no añadas al repositorio
  los artefactos generados en `powerband/target/`.
