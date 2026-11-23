---
id: integracion
type: tasks
status: in-progress
updated: 2026-11-23
---


- [x] T-301 | Núcleo `Controlador` singleton que implementa `FachadaControlador` y compone entradas, instrumentos, sintetizadores, configuración, logging y archivos para las tareas reales de integración | status: done | deps: T-133, T-219
- [x] T-302 | `main`: arranque y composición de los componentes | status: done | deps: T-301
- [x] T-303 | Conexión y desconexión: activar entradas, abrir el cliente UDP local y volver al estado inicial ante fallos locales de apertura o activación | status: done | deps: T-301
- [x] T-304 | Asignaciones en ejecución: dispositivo a instrumento e instrumento a sintetizador | status: done | deps: T-301
- [x] T-305 | Suscribir el `Grabador` al `BusComandos` y pasar el resolutor de sintetizadores al reproductor | status: done | deps: T-301
- [x] T-306 | Fallo local de transporte durante una grabación: detenerla, limpiarla, informar y volver al estado inicial | status: done | deps: T-303, T-213
- [x] T-307 | Propagar desconexiones individuales y errores a la UI mediante `ObservadorEstado` | status: done | deps: T-301, T-238
- [x] T-308 | Ciclo de vida del cierre: detener entradas, finalizar reproducción, liberar notas sostenidas y cerrar el cliente | status: done | deps: T-301, T-217, T-239
- [x] T-309 | Verificar el uso del controlador real en la UI de producción | status: done | deps: T-301, T-240
- [x] T-310 | Prueba de integración: instrumento, comando, grabación, búsqueda y reproducción con sintetizador falso | status: done | deps: T-305
- [ ] T-311 | Prueba extremo a extremo con `scsynth` y tres dispositivos distintos | status: blocked (scsynth no está disponible en PATH; la prueba con dispositivos físicos no pudo ejecutarse) | deps: T-309, T-310
- [ ] T-312 | Probar duplicado de nombre, desconexión de un dispositivo y servidor detenido durante una grabación | status: blocked (duplicado y desconexión tienen cobertura automatizada; no se puede detectar una detención remota silenciosa mediante UDP) | deps: T-311
- [x] T-313 | Ejecutar `mvn -f powerband/pom.xml clean test` y `clean package` sin errores | status: done (ambos comandos pasaron con 174 pruebas; ejecutado aunque T-312 sigue bloqueada) | deps: T-312
- [x] T-314 | Revisar los criterios de aceptación de los specs y actualizar T-005 a T-008 y `general.md` a su estado final | status: done (validación manual pendiente documentada) | deps: T-313
