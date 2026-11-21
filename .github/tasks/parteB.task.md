---
id: parteB
type: tasks
status: in-progress
updated: 2026-11-21
---

## Registros y archivos

- [x] T-201 | Clase `EncabezadoGrabacion`: `toString()`, validación de tamaño, serializar y leer | status: done | deps: R-004
- [x] T-202 | Clase `ComandoGrabado`: tiempo relativo, id de sintetizador, tipo de acción y parámetros | status: done | deps: R-004, R-009
- [x] T-203 | Clase `EntradaIndice`: nombre y posiciones inicial/final en bytes | status: done | deps: R-004
- [x] T-204 | Comparador único de nombres, usado por el ordenamiento y por la búsqueda | status: done | deps: T-203
- [x] T-205 | Lectura y escritura de registros de tamaño fijo por posición de byte; cálculo de cantidad de registros y detección de residuo (archivo inválido) | status: done | deps: T-201, T-202, T-203
- [x] T-206 | Creación de archivos de datos faltantes (`recordings.music`, `.list`, `.index`) con registro de su ubicación | status: done | deps: R-006
- [x] T-207 | Gestión de `recordings.list`: agregar en orden cronológico y listar | status: done | deps: T-204, T-205
- [x] T-208 | Búsqueda binaria en disco sobre `recordings.index` | status: done | deps: T-204, T-205
- [x] T-209 | Balanced Merge Sort sobre archivos para el índice, sin cargar todos los registros en memoria | status: done | deps: T-204, T-205
- [x] T-210 | Reemplazo del índice solo después de ordenar y validar el resultado; conservar el índice anterior ante fallos | status: done | deps: T-209

Ajuste Excepciones:
- [x] T-210-1 | Creación de nuevas clases de excepciones | status: done
- [x] T-210-2 | Implementación de excepciones en el código | status: done

## Grabación y reproducción

- [x] T-211 | Grabador: implementa `ObservadorComandos`, registra el tiempo base y escribe comandos con su tiempo relativo en tiempo real | status: done | deps: R-010, T-202, T-205
- [x] T-212 | Detener grabación: solicitar nombre, rechazar duplicados con la búsqueda, escribir encabezado y actualizar lista e índice | status: done | deps: T-211, T-207, T-208, T-210
- [x] T-213 | Limpieza de sesión incompleta ante error, sin alterar datos válidos, y registro del error | status: done | deps: T-211, R-006
- [x] T-214 | Carga: listar sesiones, buscar el nombre en el índice y leer el intervalo de bytes en `recordings.music` | status: done | deps: T-207, T-208
- [x] T-215 | Reproductor: leer comandos con buffering y enviarlos en orden respetando los tiempos relativos al inicio | status: done | deps: T-214, T-216, R-009
- [x] T-216 | Fábrica que reconstruye un `Comando` desde un `ComandoGrabado` usando un resolutor de `IdSintetizador` a `Sintetizador` | status: done | deps: T-202, R-009
- [x] T-217 | Detener la reproducción de forma controlada y reproducir los comandos de finalización de notas sostenidas | status: done | deps: T-215
- [x] T-218 | Pruebas de archivos: registros de tamaño fijo, residuo inválido, duplicados, búsqueda binaria y merge sort | status: done | deps: T-210
- [x] T-219 | Pruebas de grabar, buscar y reproducir con comandos simulados (orden e intervalos) | status: done | deps: T-217, T-212

## Interfaz gráfica (contra la fachada de R-011)

- [x] T-230 | `FachadaControlador` falsa (stub) con datos de ejemplo para desarrollar la UI sin la Parte A | status: done | deps: R-011
- [x] T-231 | Ventana principal y navegación entre paneles | status: done | deps: T-230
- [x] T-232 | Panel de conexión: iniciar y finalizar la conexión con dispositivos y sintetizadores | status: done | deps: T-231
- [ ] T-234 | Panel de asignación múltiple de sintetizadores a instrumentos | status: todo | deps: T-231
- [x] T-235 | Panel de configuración con solo las opciones modificables de `Configuraciones` | status: done | deps: T-231, R-005
- [x] T-236 | Panel de grabación: iniciar, detener, diálogo de nombre y aviso de nombre duplicado | status: done | deps: T-231
- [x] T-237 | Panel de sesiones: lista cronológica, búsqueda por nombre y reproducción | status: done | deps: T-231
- [ ] T-238 | Barra de estado y errores desde `ObservadorEstado`, sin mostrar el envío UDP como confirmación | status: todo | deps: T-231
- [ ] T-239 | Cierre de la ventana delegado al controlador (detener entradas, reproducción y notas sostenidas) | status: todo | deps: T-231
- [ ] T-240 | Revisión de la UI: sin lectura de dispositivos, sin OSC ni `BandClient`, sin algoritmos de archivos | status: todo | deps: T-239
