---
id: requerimientos
type: tasks
status: in-progress
updated: 2026-11-18
---

- [x] R-001 | Crear la estructura de paquetes (`util`, `logging`, `archivos`, `input`, `instrumentos`, `comandos`, `sintetizadores`, `controlador`, `ui`, `main`) y comprobar que compila | status: done | deps: T-002
- [x] R-002 | Verificar en `pom.xml` la dependencia de pruebas (JUnit) y la configuración de `mvn test` | status: done | deps: R-001
- [x] R-003 | Acordar los campos y tamaños fijos de `EncabezadoGrabacion`, `ComandoGrabado` y `EntradaIndice` (longitud del nombre, fecha, posiciones en bytes, tiempo relativo, id de sintetizador, tipo de acción, cantidad de parámetros) | status: done | deps: R-001
- [x] R-004 | Crear `util.Constantes` (final, constructor privado): puerto `57117`, rutas `powerband/logs` y de grabaciones, categorías `comando`/`info`/`error`, tamaños de registros de R-003 | status: done | deps: R-003
- [x] R-005 | Crear `util.Configuraciones` (final): host, puerto (por defecto `Constantes`) y opciones modificables | status: done | deps: R-004
- [x] R-006 | Crear el componente de logging: categoría, momento y `toString()` del objeto; escritura bajo `powerband/logs`; incluye ubicación en errores de archivos | status: done | deps: R-004
- [x] R-007 | Crear los enums `IdSintetizador` (piano, bajo, órgano) y `TipoAccion` (puntual, inicio, actualización, finalización) | status: done | deps: R-001
- [x] R-008 | Definir la interfaz `Sintetizador` (paquete `sintetizadores`): `id()`, ejecutar una acción con `TipoAccion` y parámetros, y liberar todas las notas | status: done | deps: R-007
- [x] R-009 | Definir la interfaz `Comando` (paquete `comandos`): `ejecutar()`, destino, tipo de acción y parámetros | status: done | deps: R-008
- [x] R-010 | Definir `ObservadorComandos` y un bus simple de comandos (`BusComandos`) para que el grabador reciba cada comando enviado con su momento | status: done | deps: R-009
- [x] R-011 | Definir la interfaz `FachadaControlador` (conectar/desconectar, listar y asignar dispositivos, asignar sintetizador a instrumento, opciones configurables, iniciar/detener grabación, listar/buscar/reproducir sesiones) y `ObservadorEstado` (estado y errores) | status: done | deps: R-009
- [x] R-012 | Acordar convenciones: nombres de clases, estilo de pruebas, ramas de git por parte y regla de no editar paquetes de la otra parte | status: done | deps: R-001