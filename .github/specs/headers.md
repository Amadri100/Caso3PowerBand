Recomiendo un front matter pequeño, con campos que cumplan una función concreta (trazabilidad y revisión), y el mismo esquema base en todos los archivos.

**1. Spec general**

```yaml
---
id: general
type: spec
scope: general
title: Arquitectura general del proyecto
status: draft            # draft | in-review | approved | implemented | deprecated
version: 0.1.0
owner: <responsable>
created: 2026-11-17
updated: 2026-11-17
depends_on: []
---
```

**2. Spec de feature**

```yaml
---
id: feat-microfono
type: spec
scope: feature
title: Entrada de micrófono con TarsosDSP
status: draft
version: 0.1.0
owner: <responsable>
created: 2026-11-17
updated: 2026-11-17
parent: general          # spec del que hereda reglas y restricciones
depends_on: [feat-util]  # otras features requeridas
packages: [input, instrumentos]
tasks: [T-010, T-011]    # ids definidos en tasks.md
---
```

**3. tasks.md**

Aquí conviene que el front matter sea de resumen y que el detalle viva en el cuerpo, con un id estable por tarea:

```yaml
---
id: tasks
type: tasks
status: in-progress
updated: 2026-11-17
---
```

Y en el cuerpo, una línea por tarea con su enlace al spec:

```markdown
- [ ] T-010 | spec: feat-microfono | Crear MicrofonoInput con thread propio | status: todo
- [x] T-011 | spec: feat-microfono | Extraer pitch con PitchProcessor | status: done
```

**4. Instructions y skills (`.github/`)**

Copilot lee `.github/copilot-instructions.md` sin front matter. Para instrucciones por ruta (`.github/instructions/*.instructions.md`) el front matter que reconoce es:

```yaml
---
applyTo: "src/main/java/**/input/**"
---
```

Los skills llevan `name` y `description`; la descripción es lo que decide cuándo se activan, así que conviene que sea específica.

**Buenas prácticas aplicadas**

- **`id` estable y único**: nunca se renombra, es la clave de referencia entre specs y tareas (por eso `tasks` apunta a `spec`, y el spec a `tasks`).
- **`status` con valores cerrados**: documente la lista en el spec general. En su ejemplo está en español (`desarrollo`); elija un idioma y un vocabulario y manténgalos, porque inconsistencias como `desarrollo`/`en desarrollo` rompen cualquier validación automática.
- **`parent` y `depends_on`**: dan jerarquía y orden de implementación sin tener que leer el cuerpo.
- **`version` y `updated`**: permiten saber si un spec cambió después de implementarse. Súbale la versión cuando cambie un requisito, no por correcciones de redacción.
- **Sin duplicar contenido**: el front matter son metadatos; los requisitos van en el cuerpo. Evite listas largas en el encabezado.
- **Validación**: un pequeño script o una GitHub Action que verifique que los `id` existen y que los `tasks` referenciados están en `tasks.md` mantiene la trazabilidad sin esfuerzo manual.
- **Espacios en YAML**: use `clave: valor` sin espacio antes de los dos puntos. Su ejemplo usa `id : general`, que muchos parsers aceptan pero es poco estándar.

Un detalle del cuerpo de su spec: `### intrumentos` y `### sintetizadores` tienen erratas (`intrumentos`, `syntetizador`), y las referencias a esos nombres desde otros archivos deben coincidir exactamente con el nombre del paquete real.