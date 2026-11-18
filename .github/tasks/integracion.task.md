---
id: integracion
type: tasks
status: in-progress
updated: 2026-11-18
---


- [ ] T-301 | `Controlador` con instancia única que implementa `FachadaControlador` y compone entradas, instrumentos, sintetizadores, configuración, logging y archivos | status: todo | deps: T-133, T-219
- [ ] T-302 | `main`: arranque y composición de los componentes | status: todo | deps: T-301
- [ ] T-303 | Conexión y desconexión: activar instrumentos, abrir `BandClient` y volver al estado inicial si el servidor no inicia | status: todo | deps: T-301
- [ ] T-304 | Asignaciones en ejecución: dispositivo a instrumento e instrumento a sintetizador | status: todo | deps: T-301
- [ ] T-305 | Suscribir el `Grabador` al `BusComandos` y pasar el resolutor de sintetizadores al reproductor | status: todo | deps: T-301
- [ ] T-306 | Fallo del servidor durante una grabación: detenerla, limpiarla, informar y volver al estado inicial | status: todo | deps: T-303, T-213
- [ ] T-307 | Propagar desconexiones individuales y errores a la UI mediante `ObservadorEstado` | status: todo | deps: T-301, T-238
- [ ] T-308 | Ciclo de vida del cierre: detener entradas, finalizar reproducción, liberar notas sostenidas y cerrar el cliente | status: todo | deps: T-301, T-217, T-239
- [ ] T-309 | Reemplazar el stub de la UI por el controlador real | status: todo | deps: T-301, T-240
- [ ] T-310 | Prueba de integración: instrumento, comando, grabación, búsqueda y reproducción con sintetizador falso | status: todo | deps: T-305
- [ ] T-311 | Prueba extremo a extremo con `scsynth` y tres dispositivos distintos | status: todo | deps: T-309, T-310
- [ ] T-312 | Probar duplicado de nombre, desconexión de un dispositivo y servidor detenido durante una grabación | status: todo | deps: T-311
- [ ] T-313 | Ejecutar `mvn -f powerband/pom.xml clean test` y `clean package` sin errores | status: todo | deps: T-312
- [ ] T-314 | Revisar los criterios de aceptación de los specs y actualizar T-005 a T-008 y `general.md` a su estado final | status: todo | deps: T-313
