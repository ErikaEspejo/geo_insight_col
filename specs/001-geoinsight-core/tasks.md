---

description: "Task list template for feature implementation"
---

# Tasks: Sistema Núcleo de GeoInsight Colombia

**Input**: Design documents from `/specs/001-geoinsight-core/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: AGENTS.md exige tests ante cada cambio de comportamiento. Se generan tasks de test por historia (tests de dominio primero, luego casos de uso). Escribir los tests ANTES de la implementación de su historia (TDD) y verificar que fallen.

**Organization**: Tasks grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Proyecto único Maven con paquetes por capa:

```text
src/main/java/co/edu/distrital/geoinsight/{domain,application,infrastructure,web}
src/main/resources/static/            # frontend (Leaflet)
src/test/java/co/edu/distrital/geoinsight/
data/                                 # escribible (gitignored)
config/admin-account.json             # cuenta admin sembrada
docs/datasets/                        # datasets SGC (gitignored)
scripts/download-datasets.ps1         # descarga manual (base del bootstrap)
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialización del proyecto Maven y estructura base

- [x] T001 Create Maven project: `pom.xml` con Java 21, `spring-boot-starter-parent` 3.3.x y dependencias `web`, `security`, `validation`, `test` (JUnit 5 + AssertJ). `groupId co.edu.distrital`, `artifactId geo-insight-col`
- [x] T002 Configure Maven Wrapper (`mvnw.cmd`, `mvnw`, `.mvn/wrapper/maven-wrapper.properties`) con Maven 3.9.x
- [x] T003 [P] Application config: `src/main/resources/application.properties` (server.port=8080, rutas configurables: `geoinsight.data-dir=data`, `geoinsight.datasets-dir=docs/datasets`, `geoinsight.admin-account-file=config/admin-account.json`)
- [x] T004 [P] Vendor Leaflet 1.9.x: descargar `leaflet.js`, `leaflet.css` y `images/` en `src/main/resources/static/lib/leaflet/` (local, sin CDN)
- [x] T005 [P] Create `config/admin-account.json` con `username: "admin"` y `passwordHash` BCrypt de la contraseña por defecto (documentar la contraseña en el archivo de tareas y en quickstart.md; p. ej. `admin123`)
- [x] T006 [P] Update `.gitignore`: ignorar `data/` (users.json, geoentities.json)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Núcleo de dominio + infraestructura que BLOQUEAN toda historia de usuario

**CRITICAL**: Ninguna user story puede comenzar hasta completar esta fase

- [x] T007 [P] Domain geometry: `Coordinate` (record, valida lon[-180,180]/lat[-90,90], `distanceTo` haversine en metros) en `src/main/java/co/edu/distrital/geoinsight/domain/geometry/Coordinate.java`
- [x] T008 [P] Domain geometry hierarchy: `Geometry` abstracta (`distanceTo(Coordinate)`, `contains(Coordinate)`, `bounds()`) con `Point`, `LineString`, `Polygon` (anillo + agujeros), `MultiPoint`, `MultiLineString`, `MultiPolygon`, y `GeometryFactory` en `src/main/java/co/edu/distrital/geoinsight/domain/geometry/` (ray casting para contención; distancia punto-a-segmento; multiparte evalúa cada parte)
- [x] T009 [P] Domain model: `Domain` (enum 5 dominios con atributos obligatorios/admitidos), `Origin` (SGC/GEOINSIGHT), `GeoscienceEntity` (id, domain, origin, geometry, `Map<String,Object>` attributes; invariantes), `Zone` (record centro+radio validado) en `src/main/java/co/edu/distrital/geoinsight/domain/model/`
- [x] T010 [P] Domain repository interfaces: `DatasetRepository` (entidades por dominio, metadatos de capa), `GeoEntityRepository` (CRUD GEOINSIGHT), `UserAccountRepository` en `src/main/java/co/edu/distrital/geoinsight/domain/repository/`
- [x] T011 [P] Domain unit tests (TDD, deben fallar antes de T007-T009): distancia haversine, contención polígono/multiparte/agujeros, distancia punto-a-línea, invariantes de entidad en `src/test/java/co/edu/distrital/geoinsight/domain/`
- [x] T012 Infrastructure dataset loader: `GeoJsonDatasetRepository` (Jackson) parsea los 5 GeoJSON de `docs/datasets/`, valida conteos esperados (61/4866/7461/3/6826), expone entidades por dominio en `src/main/java/co/edu/distrital/geoinsight/infrastructure/persistence/`
- [x] T013 [P] Infrastructure downloader + bootstrap: `SgcDatasetDownloader` (java.net.http, paginación resultOffset/resultRecordCount, outSR=4326, verificación de conteo) replicando `scripts/download-datasets.ps1`, y `DatasetBootstrapService` (verifica/descarga al arranque) en `src/main/java/co/edu/distrital/geoinsight/infrastructure/download/` y `infrastructure/bootstrap/`
- [x] T014 [P] Infrastructure `JsonGeoEntityRepository` (CRUD sobre `data/geoentities.json`, escritura atómica temp+rename) en `src/main/java/co/edu/distrital/geoinsight/infrastructure/persistence/`
- [x] T015 [P] Infrastructure `JsonUserAccountRepository` + `AdminAccountSeeder` (siembra admin desde `config/admin-account.json` si no existe en `data/users.json`) en `src/main/java/co/edu/distrital/geoinsight/infrastructure/persistence/`
- [x] T016 [P] Infrastructure tests (TDD): loader sobre archivos reales (conteos y atributos), downloader con servidor HTTP de prueba (pagina y verifica), repositorios round-trip en `src/test/java/co/edu/distrital/geoinsight/infrastructure/`
- [x] T017 Web security skeleton: `SecurityConfig` (Spring Security, sesión HTTP, `PasswordEncoder` BCrypt, CSRF deshabilitado por API JSON local y rutas públicas limitadas al login) en `src/main/java/co/edu/distrital/geoinsight/web/security/`

**Checkpoint**: Fundación lista: dominio, datos cargados, persistencia, seguridad base. Las historias pueden comenzar.

---

## Phase 3: User Story 1 - Autenticación y registro (Priority: P1) — MVP

**Goal**: Todo acceso exige sesión; registro de usuarios de consulta (rol USER, nunca ADMIN); admin sembrado; sesión identifica rol.

**Independent Test**: Un visitante sin sesión no llega al mapa (redirige a login); registro → rol USER; login admin → rol ADMIN; un USER recibe 403 en `/api/admin/**` (escenario E1 de quickstart.md).

### Tests for User Story 1

- [x] T018 [P] [US1] Tests de dominio/aplicación (TDD, fallan antes): registro siempre crea rol USER, usuario duplicado rechazado, verificación BCrypt, admin sembrado solo si no existe en `src/test/java/co/edu/distrital/geoinsight/application/auth/`

### Implementation for User Story 1

- [x] T019 [US1] `AuthenticationService` y `RegistrationService` en `src/main/java/co/edu/distrital/geoinsight/application/auth/` (BCrypt, validación de credenciales, duplicados → excepción de conflicto, invariante "registro nunca admin")
- [x] T020 [US1] `AuthController` (POST `/api/auth/register`, POST `/api/auth/login`, POST `/api/auth/logout`, GET `/api/auth/me`) en `src/main/java/co/edu/distrital/geoinsight/web/controller/AuthController.java` con DTOs validados
- [x] T021 [US1] Completar `SecurityConfig`: `/api/admin/**` exige rol ADMIN, `/api/**` autenticado, login JSON, estáticos protegidos salvo `login.html` y `/api/auth/**` en `src/main/java/co/edu/distrital/geoinsight/web/security/SecurityConfig.java`
- [x] T022 [US1] Frontend login/registro: `src/main/resources/static/login.html` + `static/js/auth.js` (formularios, manejo de errores 401/409, redirección)

**Checkpoint**: US1 funcional e independientemente testeable.

---

## Phase 4: User Story 2 - Exploración cartográfica por capas (Priority: P1) — MVP

**Goal**: Mapa con 5 capas diferenciadas, activar/desactivar, selección de entidad con atributos reales y procedencia, filtros solo por atributos reales.

**Independent Test**: Con sesión, el mapa muestra las cinco capas; al desactivar una se ocultan sus entidades; al seleccionar se muestran atributos reales + procedencia; filtros solo de atributos reales (escenarios E2 de quickstart.md).

### Tests for User Story 2

- [x] T023 [P] [US2] Tests de aplicación (TDD, fallan antes): metadatos de capa reflejan atributos reales, filtro con atributo inexistente rechazado, detalle incluye origen en `src/test/java/co/edu/distrital/geoinsight/application/exploration/`

### Implementation for User Story 2

- [x] T024 [P] [US2] `LayerExplorationService` en `src/main/java/co/edu/distrital/geoinsight/application/exploration/` (metadatos por dominio con atributos filtrables y sus dominios de valores derivados, entidades por dominio con filtro, detalle por id)
- [x] T025 [P] [US2] Obtener contorno de Colombia (GeoJSON simplificado, límites de bajo peso) y guardarlo en `src/main/resources/basemap/colombia.geojson` (fondo vectorial offline)
- [x] T026 [US2] `LayerController` (GET `/api/layers`, `/api/layers/{domain}/geojson`, `/api/entities/{domain}?attr=valor`, `/api/entities/{domain}/{id}`) y `BasemapController` (GET `/api/basemap/colombia`) en `src/main/java/co/edu/distrital/geoinsight/web/controller/` con DTOs
- [x] T027 [US2] Frontend mapa: `src/main/resources/static/index.html` + `static/js/map.js` (Leaflet, 5 capas desde la API, control de capas, fondo vectorial Colombia + tiles OSM si hay red, selección de entidad) + `static/js/api.js` (cliente REST)
- [x] T028 [US2] Frontend atributos y filtros: panel de atributos con procedencia y UI de filtros construida SOLO con atributos reales de `/api/layers` en `static/js/` + `static/css/styles.css`
- [x] T029 [US2] Tests web (TDD): 401 sin sesión, GeoJSON por dominio, filtro con atributo inexistente → 400 en `src/test/java/co/edu/distrital/geoinsight/web/`

**Checkpoint**: US1 + US2 forman el MVP completo (mapa explorable tras login).

---

## Phase 5: User Story 3 - Consulta del contexto de una coordenada (Priority: P1)

**Goal**: Dada una coordenada, contexto por dominio: unidades/dominios contenientes, falla/movimiento/volcán más cercanos con distancia, ausencia explícita.

**Independent Test**: `POST /api/context` devuelve resultados por dominio con distancia o ausencia explícita; coordenada inválida → 400 (escenario E3 de quickstart.md).

### Tests for User Story 3

- [x] T030 [P] [US3] Tests de aplicación: cascada de cobertura, fuera de cobertura → null y desempate por identificador completo en `src/test/java/co/edu/distrital/geoinsight/application/analysis/`

### Implementation for User Story 3

- [x] T031 [US3] `CoordinateContextService` en `src/main/java/co/edu/distrital/geoinsight/application/analysis/` (unidades contenientes, dominios contenientes, más cercanos por distancia mínima punto-geometría, empates deterministas, ausencia explícita)
- [x] T032 [US3] Extender `AnalysisController`: POST `/api/context` con DTO validado en `src/main/java/co/edu/distrital/geoinsight/web/controller/AnalysisController.java`
- [x] T033 [US3] Frontend: formulario de coordenada + clic en el mapa + panel de resultados por dominio en `src/main/resources/static/js/context.js` + `index.html`
- [x] T033a [US3] Presentación por secciones (Resultado / Contexto geológico / Elementos cercanos) con tarjetas compactas, nombres descriptivos priorizados e identificadores secundarios, distancias m/km con un decimal y mensajes de ausencia legibles en `js/context.js` + `css/context.css`
- [x] T033b [US3] Relación con el mapa: marcador propio de la ubicación consultada, resaltado de contenedores y de falla/movimiento/volcán más cercanos (panes `contextContainers`/`contextNearest`), vista ajustada a los elementos resaltados desde el formulario y conservada en el clic en `js/map.js`

**Checkpoint**: US3 funcional e independientemente testeable.

---

## Phase 6: User Story 4 - Análisis de una zona (Priority: P2)

**Goal**: Zona (centro + radio) → conteos y distribuciones por dominio, listados; ausencia explícita; sin frases de riesgo.

**Independent Test**: `POST /api/zones/analyze` devuelve conteos/distribuciones/listados por dominio; zona sin registros → count 0; radio inválido → 400; sin conclusión de riesgo (escenario E4).

### Tests for User Story 4

- [x] T034 [P] [US4] Tests de aplicación (TDD, fallan antes): conteos y distribuciones por `TIPO`/`SUBTIPO`/`CLAS_MAPA`, dominio vacío → count 0, radio inválido rechazado, salida sin palabras de riesgo en `src/test/java/co/edu/distrital/geoinsight/application/analysis/`

### Implementation for User Story 4

- [x] T035 [US4] `ZoneAnalysisService` en `src/main/java/co/edu/distrital/geoinsight/application/analysis/` (movimientos/volcanes a distancia ≤ radio, fallas a distancia centro-línea ≤ radio, unidades/dominios que contienen el centro o intersectan la circunferencia; indicadores descriptivos)
- [x] T036 [US4] Extender `AnalysisController`: POST `/api/zones/analyze` (validación radio > 0) en `src/main/java/co/edu/distrital/geoinsight/web/controller/AnalysisController.java`
- [x] T037 [US4] Frontend: formulario de zona + círculo (centro+radio) en el mapa + panel de indicadores en `src/main/resources/static/js/zone.js` + `index.html`
- [x] T037a [US4] Extender distribuciones por dominio al atributo de clasificación real de cada dataset (fallas `Tipo`, unidades `Edad`, dominios `NombreDT`; volcanes sin distribución) y agrupar registros sin el atributo en `Sin clasificar` (suma = `count`), con contrato actualizado en `contracts/api.md` y cobertura en `ZoneBreakdownTest`/`AnalysisWebTest`.
- [x] T037b [US4] Rediseñar el panel de análisis de zona: bloques por dominio con distribución, tarjetas filtrables multi-selección que resaltan en el mapa y resumen «N dominios en el mapa», barras interactivas solo en movimientos en masa.

**Checkpoint**: US4 funcional e independientemente testeable.

---

## Phase 7: User Story 5 - Comparación descriptiva de dos zonas (Priority: P2)

**Goal**: Dos zonas con los mismos indicadores lado a lado; diferencias solo descriptivas.

**Independent Test**: `POST /api/zones/compare` muestra el mismo esquema en ambas columnas (escenario E5).

### Tests for User Story 5

- [x] T038 [P] [US5] Tests de aplicación (TDD, fallan antes): mismo conjunto de indicadores en ambas zonas, diferencias descriptivas en `src/test/java/co/edu/distrital/geoinsight/application/analysis/`

### Implementation for User Story 5

- [x] T039 [US5] `ZoneComparisonService` en `src/main/java/co/edu/distrital/geoinsight/application/analysis/` (reutiliza `ZoneAnalysisService` y limita los vecinos cercanos a las entidades del radio)
- [x] T040 [US5] Extender `AnalysisController`: POST `/api/zones/compare` en `src/main/java/co/edu/distrital/geoinsight/web/controller/AnalysisController.java`
- [x] T041 [US5] Frontend: UI de comparación (dos zonas, dos círculos, columnas lado a lado) en `src/main/resources/static/js/compare.js` + `index.html`

**Checkpoint**: US5 funcional e independientemente testeable.

---

## Phase 8: User Story 6 - Incorporación y gestión de entidades propias (Priority: P3)

**Goal**: Admin crea/edita/elimina entidades GEOINSIGHT con campos obligatorios por dominio; SGC inmutable; USER sin acceso.

**Independent Test**: Admin crea entidad GEOINSIGHT (201), edita/elimina (persiste), intento sobre SGC → 403, USER → 403 (escenario E6).

### Tests for User Story 6

- [x] T042 [P] [US6] Tests de aplicación (TDD, fallan antes): creación con campos obligatorios, falta de campo → rechazo, geometría no admitida → rechazo, SGC inmutable, actualización/eliminación persisten en `src/test/java/co/edu/distrital/geoinsight/application/admin/`

### Implementation for User Story 6

- [x] T043 [US6] `GeoEntityManagementService` en `src/main/java/co/edu/distrital/geoinsight/application/admin/` (campos obligatorios por dominio desde datasets, geometría admitida por dominio, IDs `GEO-{uuid}`, protección de entidades SGC)
- [x] T044 [US6] `AdminController` (POST `/api/admin/entities`, PUT `/api/admin/entities/{id}`, DELETE `/api/admin/entities/{id}`) en `src/main/java/co/edu/distrital/geoinsight/web/controller/AdminController.java`
- [x] T045 [US6] Frontend admin: formularios de crear/editar/eliminar visibles SOLO para rol ADMIN en `src/main/resources/static/js/admin.js` + `index.html`
- [x] T046 [US6] Tests web (TDD): 401 sin sesión, 403 para USER, 403 sobre entidad SGC, 201/200/204 para ADMIN en `src/test/java/co/edu/distrital/geoinsight/web/`

**Checkpoint**: US6 funcional e independientemente testeable.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Mejoras transversales y validación final

- [x] T049 [P] Documentación: actualizar `README.md` (cómo ejecutar, credenciales por defecto del admin sembrado, arquitectura backend+frontend, rutas de datos)
- [x] T050 Build final completo: `./mvnw.cmd clean verify` con todos los tests en verde

## Phase 10: UX geocientífica y administración segura

**Purpose**: Sincronizar la experiencia implementada con FR-026..FR-042 sin alterar las reglas espaciales.

- [x] T068 [FR-050] Ejecutar `BootstrapRunner` durante la inicialización de
  singletons, antes de que Tomcat acepte conexiones, para no exponer el estado
  transitorio de datasets aún no descargados o cargados.
- [x] T069 [FR-051] Reintentar cada descarga hasta tres intentos configurables,
  con espera incremental y prueba de recuperación después de un error HTTP
  transitorio.

> **Estado actualizado (2026-08-16)**: T047a, T047b y T048-R se registran como
> completadas. La evidencia automatizada y la validación manual confirmada por
> el usuario están registradas en `validation.md`.

- [x] T047a [P] Presupuestos backend SC-001..SC-003 con datasets reales:
  GeoJSON pesado <10 s, contexto <5 s y zona <5 s.
- [x] T047b Medir en navegador el render completo de unidades (<10 s) y
  registrar entorno/resultado en `validation.md`.
- [x] T048-R Ejecutar E1–E9, incluida la prueba offline, y registrar evidencia
  visual en `validation.md`.

- [x] T051 [US2] Capas inicialmente inactivas, render Canvas para puntos densos y control de respuestas asíncronas obsoletas en `static/js/map.js`.
- [x] T052 [US2] Filtros OR por valores del mismo atributo y AND entre atributos; limpieza al cambiar capa; tabla de resultados con zoom en `LayerExplorationService`, `LayerController` y `static/js/map.js`.
- [x] T053 [US3/US4/US5] Selector de coordenada explícito por formulario y consulta puntual limitada a su módulo.
- [x] T054 [US4/US5] Centros, radios diferenciados y ajuste de viewport para análisis y comparación.
- [x] T055 [US5] Comparación radial mediante reutilización de `ZoneAnalysisService`; vecinos limitados a las entidades incluidas por el radio, disponibilidad explícita, tarjetas por dominio y observaciones solo descriptivas.
- [x] T056 [US6] Lista blanca de atributos descriptivos, `AttributeValueType` derivado y validación de claves/tipos en backend; metadata administrativa en `GET /api/layers`.
- [x] T057 [US6] Formulario administrativo dinámico con valores categóricos reales y controles tipados; eliminación de captura JSON libre.
- [x] T058 [US6] Dibujo Point/LineString/Polygon sin dependencia externa; lista/capa “Mis entidades”, colores por dominio y modal propio de eliminación.
- [x] T059 [US1..US6] Rediseño responsive de login con recursos públicos y sin SSO, shell sin controles redundantes, placeholders de coordenadas, detalle legible y ayuda contextual por rol.
- [x] T060 Pruebas de regresión y contrato actualizadas; suite completa verificada con 100 tests en verde.
- [x] T061 [US2] Mover el selector de capas a un control flotante en la esquina inferior derecha del mapa, con icono convencional de capas apiladas, etiqueta «Capas», desplegable que abre hacia arriba y cierre por clic fuera o Escape, conservando tooltip, etiqueta accesible, foco visible y estado activo.
- [x] T062 [US2] Eliminar la herramienta lateral «Explorar mapa» y su panel; la lista de capas queda únicamente en el control flotante y la ayuda se actualiza a «Capas del mapa».
- [x] T063 [US2] Inicializar el panel contextual colapsado con «Buscar y filtrar» como módulo predeterminado, sincronizando chevrón y etiqueta del botón de colapso con el estado cerrado.
- [x] T064 [US2/US4/US5] Vista previa de entidad al pasar el cursor: tooltip de Leaflet en líneas/polígonos y hit-testing en puntos canvas (mismo tooltip que el clic) en `static/js/map.js` + `css/styles.css`.
- [x] T065 [US3/US4/US5] Acción «Borrar» en consulta por coordenada, análisis de zona y comparación: restablece el panel a su estado vacío y limpia los overlays propios del análisis en `index.html`, `context.js`, `zone.js`, `compare.js`.
- [x] T066 [US5] Unificar el contrato de comparación con un único `radiusMeters` común para ambas coordenadas en `CompareRequest`, `AnalysisController`, `contracts/api.md`, `compare.js` y pruebas web.
- [x] T067 Sincronizar constitución 1.0.1, plan, research, modelo de datos, checklist y README con las decisiones implementadas y la ubicación oficial de artefactos Spec Kit.

## Phase 11: Evidencia académica SDD

**Purpose**: Hacer explícita y auditable la derivación desde los datos y la
especificación hasta el diseño, el código y las pruebas.

- [x] T070 [P] Documentar el diseño OO en `design.md` con diagramas Mermaid de
  clases y arquitectura, incluyendo herencia, polimorfismo, encapsulamiento,
  composición e interfaces de repositorio.
- [x] T071 [P] Perfilar los cinco GeoJSON completos y registrar en
  `data-dictionary.md` los 46 campos con tipo observado, nulabilidad, blancos,
  cardinalidad, clasificación y restricciones de captura.
- [x] T072 Crear `traceability.md` con la cadena datos → FR/SC → diseño y
  contrato → tareas → implementación → pruebas, cubriendo FR-001..FR-051 y
  SC-001..SC-011.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias — puede comenzar de inmediato
- **Foundational (Phase 2)**: Depende de Setup — BLOQUEA todas las user stories
- **User Stories (Phase 3+)**: Todas dependen de Foundational
  - US1 (P1) → US2..US6 (autenticación es prerrequisito de acceso)
  - US2 (P1) → base para US3 (mapa/coordenadas)
  - US3 (P1) → US4 (zona amplía la consulta puntual)
  - US4 (P2) → US5 (comparación usa análisis de zona)
  - US6 (P3) → independiente, requiere US1 (admin autenticado)
- **Polish (Phase 9)**: Depende de todas las historias deseadas
- **Sincronización UX (Phase 10)**: Depende de las historias completas y extiende sus criterios de aceptación sin cambiar la semántica espacial

### User Story Dependencies

- **US1 (P1)**: Bloquea el acceso a todo; sin ella ninguna historia es usable
- **US2 (P1)**: Depende de US1 en el flujo (el mapa exige sesión) pero su lógica es independiente
- **US3 (P1)**: Depende de US2 (frontend de mapa) para selección por clic; la API es independiente
- **US4 (P2)**: Reutiliza las operaciones espaciales de US3
- **US5 (P2)**: Depende de US4 (reutiliza `ZoneAnalysisService`)
- **US6 (P3)**: Depende solo de Foundational + US1

### Within Each User Story

- Tests (si se incluyen) DEBEN escribirse y FALLAR antes de la implementación
- Modelos/dominio antes que servicios; servicios antes que endpoints
- Implementación antes que integración frontend

### Parallel Opportunities

- Setup: T003–T006 en paralelo
- Foundational: T007–T011 (dominio) y T012–T016 (infraestructura) en paralelo tras definir interfaces (T010); T017 seguridad en paralelo
- Tras Foundational, cada user story puede trabajarse secuencialmente en prioridad (P1→P3) por un único implementador
- Dentro de cada historia: tests [P] primero, luego tareas marcadas [P] en paralelo cuando no comparten archivos

---

## Parallel Example: User Story 3

```text
Task: "Tests de aplicación (TDD) en src/test/.../application/analysis/"
Task: "CoordinateContextService en src/main/.../application/analysis/"
```

(Los tests se escriben primero y fallan; luego la implementación.)

---

## Implementation Strategy

### MVP (User Story 1 + User Story 2)

1. Phase 1: Setup
2. Phase 2: Foundational (CRÍTICO — bloquea todo)
3. Phase 3: US1 (autenticación) → valida E1
4. Phase 4: US2 (mapa por capas) → valida E2
5. **STOP y VALIDAR**: el MVP ya permite explorar el mapa tras login

### Incremental Delivery

1. Setup + Foundational → Fundación
2. US1 → E1 → demo
3. US2 → E2 → demo (MVP)
4. US3 → E3 → demo (consulta por coordenada)
5. US4 → E4 → demo (análisis de zona)
6. US5 → E5 → demo (comparación)
7. US6 → E6 → demo (gestión admin)
8. Polish → E7, E8, build final

### Parallel Team Strategy

Con varios desarrolladores: equipo completa Setup + Foundational juntos; luego un desarrollador por historia (US1/US2/US3 en paralelo, US4/US5/US6 después por dependencias).

---

## Notes

- [P] tasks = archivos diferentes, sin dependencias
- [Story] label mapea la tarea a su user story (trazabilidad P-08)
- Cada user story es completable y testeable de forma independiente
- Verificar que los tests fallen antes de implementar
- Commit después de cada tarea o grupo lógico
- Evitar: tareas vagas, conflictos de archivos, dependencias cruzadas que rompan independencia
- El frontend DEBE consultar solo los atributos reales que expone `/api/layers` (nunca hardcodear campos inexistentes)
- `docs/datasets/` es gitignored: los tests de infraestructura que dependen de archivos reales deben omitirse si el dataset no está presente (assumption de dataset-driven design)
