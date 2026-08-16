# Implementation Plan: Sistema Núcleo de GeoInsight Colombia

**Branch**: `001-geoinsight-core` | **Date**: 2026-08-15 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-geoinsight-core/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Se construye **GeoInsight Colombia** como una **aplicación web local** con backend en Java 21 (Spring Boot, REST) y frontend web (HTML/JS + Leaflet). Integra cinco dominios geocientíficos del SGC (inmutables, solo lectura) y entidades propias `GEOINSIGHT` gestionables únicamente por el administrador. Todo acceso exige autenticación: la cuenta administrador se siembra en un JSON de configuración y los usuarios de consulta pueden registrarse (rol usuario, nunca admin). Al iniciar, el sistema verifica y, si es necesario, descarga automáticamente los cinco datasets desde las APIs REST oficiales del SGC (requiere internet solo la primera vez). El backend es dueño de toda la lógica geoespacial (contención, proximidad, zona, comparación); el frontend visualiza con Leaflet y un fondo vectorial local (Colombia) para funcionar también sin conexión.

## Technical Context

**Language/Version**: Java 21 (LTS) — verificado en la máquina local (`21.0.11`).

**Build**: Apache Maven 3.9.16 (verificado). Se configura el Maven Wrapper (`mvnw`) para reproducibilidad.

**Primary Dependencies**:
- Spring Boot 3.3.x: `spring-boot-starter-web` (REST + servido de estáticos), `spring-boot-starter-security` (sesión + BCrypt), `spring-boot-starter-validation`.
- Jackson (geojson-jackson deserialización): `jackson-databind` (incluido por Spring Boot).
- Leaflet 1.9.x: distribuido como archivos estáticos locales en `src/main/resources/static/lib/leaflet/` (vendoreado, sin CDN).
- BCrypt: `spring-security-crypto` (incluido en starter-security).
- Tests: JUnit 5, AssertJ (incluidos en `spring-boot-starter-test`); Mockito solo si el aislamiento es necesario.
- No se usan: bases de datos, JPA, Hibernate, Spring Data.

**Storage**: Archivos JSON locales, todos detrás de interfaces de repositorio:
- Datasets SGC (solo lectura): `docs/datasets/*.geojson` (gitignored), cargados en memoria al arranque.
- Entidades GEOINSIGHT (escribible): `data/geoentities.json`.
- Cuentas de usuario (escribible): `data/users.json`.
- Cuenta administrador sembrada (configuración): `config/admin-account.json` (usuario + hash BCrypt).

**Testing**: JUnit 5 + AssertJ vía `./mvnw test` y `./mvnw verify`; prioridad 1: tests de dominio (geometría, reglas, invariantes); prioridad 2: tests de casos de uso.

**Target Platform**: Escritorio local (Windows), servido en `http://localhost:8080`, navegador moderno.

**Project Type**: Web application (backend + frontend), un solo artefacto Maven que sirve la API REST y el frontend estático.

**Performance Goals**: SC-001 (mapa con 5 capas < 10 s), SC-002 (consulta por coordenada < 5 s), SC-003 (análisis de zona < 5 s).

**Constraints**:
- Dominio 100% independiente de Spring, Jackson, HTTP, JSON, Leaflet y frontend.
- Datos SGC inmutables; solo el admin gestiona `GEOINSIGHT`.
- Sin cálculos ni mensajes de riesgo/amenaza/peligrosidad/seguridad/predicciones.
- Los diccionarios de datos (campos filtrables, obligatorios, dominios de valores) se derivan de los archivos reales.
- Sin repositorios de datos adicionales (no DB/JPA/Spring Data).

**Scale/Scope**: 5 dominios, ~19.000 entidades SGC (121.7 MB decimales en total: 107.8 MB unidades, 7.4 MB fallas, 2.3 MB movimientos, 4.1 MB dominios y <0.1 MB volcanes) + entidades GEOINSIGHT; 2 roles (usuario, administrador); un usuario activo.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**P-01 Spec-First**: el plan deriva exclusivamente de `spec.md`; cualquier decisión no cubierta vuelve a la spec. Los diccionarios de datos se derivan de los datasets reales (FR-013, FR-018), no se inventan.

**P-02 OOP significativo**: la geometría y la entidad geocientífica se modelan como objetos con comportamiento real (cálculo de distancia, contención, invariantes de procedencia). No se crean jerarquías artificiales: los cinco dominios comparten comportamiento idéntico y difieren solo en datos, por lo que se modelan con un enum `Domain` y atributos dinámicos, no con 5 subclases.

**P-03 Datos reales, no inventados**: campos, tipos, valores y geometrías provienen de inspección de los 5 GeoJSON. Contención/proximidad/distancias se definen con semántica explícita y comprobable.

**P-04 Independencia del dominio**: `domain/` no importa Spring, Jackson, HTTP, archivos ni Leaflet. La carga (parsing GeoJSON), el bootstrap de descarga y la persistencia viven en `infrastructure/`. La web vive en `web/`. La lógica de análisis vive en `application/` sobre el dominio.

**P-05 Corrección científica**: sin riesgo/amenaza/predicción; las ausencias son explícitas; los resultados son descriptivos; la procedencia siempre visible.

**P-06 Simplicidad (KISS/YAGNI)**: un solo artefacto Maven, paquetes por capa, sin abstracciones especulativas. El repositorio es la única abstracción de persistencia requerida. No se introducen módulos multi-artefacto ni frameworks de frontend (sin Angular/React).

**P-07 Testing por contratos**: tests de dominio sobre reglas reales; los casos de uso se prueban con fakes de repositorio; verificación final `./mvnw verify`.

**P-08 Trazabilidad**: cada tarea referencia su FR y user story; los resultados del análisis se pueden trazar a sus entidades de origen.

**Gates**:
- [x] Sin violaciones a la constitución detectadas. (Complejidad adicional justificada abajo si aparece.)

## Phase 0: Research (`research.md`)

Resuelve todos los puntos del Technical Context y las semánticas geoespaciales (ver `research.md`). Nada queda como `NEEDS CLARIFICATION`.

## Phase 1: Design

### Dominio (`src/main/java/co/edu/distrital/geoinsight/domain/`)

El diagrama de clases y las relaciones de herencia, composición e
implementación de interfaces están formalizados en [design.md](./design.md).

- `Coordinate` (record): `lon, lat` validados (rango WGS84) + distancia haversine a otra coordenada.
- Geometría OOP (`Geometry` abstracta; `Point`, `LineString`, `Polygon`, `MultiPoint`, `MultiLineString`, `MultiPolygon`), con comportamiento:
  - `distanceTo(Coordinate)` (mínima distancia punto-geometría, haversine + punto-segmento).
  - `contains(Coordinate)` (punto-en-polígono por ray casting; multiparte evalúa cada parte).
  - `bounds()` (bbox) como información geométrica; los análisis actuales usan recorridos lineales porque el volumen medido cumple SC-002/SC-003 sin un filtro espacial adicional.
- `Domain` (enum): `MOVIMIENTO_EN_MASA`, `FALLA_GEOLOGICA`, `UNIDAD_GEOLOGICA`, `DOMINIO_TECTONICO`, `VOLCAN`.
- `Origin` (enum): `SGC`, `GEOINSIGHT`.
- `GeoscienceEntity`: id, `Domain`, `Origin`, `Geometry`, atributos `Map<String,Object>`. Invariante: origen y dominio inmutables.
- `Zone` (record): `Coordinate centro` + `double radioMetros` (finito, validado > 0 y sin máximo).
- `UserAccount` / `Role` (enum `USER`, `ADMIN`): cuenta con usuario, hash BCrypt, rol.
- `GeometryFactory`: construcción de geometrías desde coordenadas (evita estados inválidos).

### Aplicación (`application/`)

- `auth/AuthenticationService`: login (verificación BCrypt), logout, registro (rol siempre USER, rechazo de usuario duplicado), consulta de sesión actual. Protege el invariant "cuentas registradas nunca son admin".
- `exploration/LayerExplorationService`: listado de dominios + metadatos de capa (atributos filtrables y sus dominios de valores derivados), entidades por dominio con filtro, detalle de entidad por id.
- `analysis/CoordinateContextService`: determina cobertura por disponibilidad (dominios tectónicos, unidades geológicas, basemap) y, dentro de ella, obtiene contenedores y vecinos más cercanos con desempate lexicográfico. Fuera de cobertura devuelve ausencia explícita.
- `analysis/ZoneAnalysisService`: para una zona (centro+radio): conteos y distribuciones de movimientos dentro del radio, fallas cuya distancia al centro ≤ radio, unidades y dominios tectónicos que intersectan la zona, volcanes dentro del radio. Sin frases de riesgo.
- `analysis/ZoneComparisonService`: reutiliza `ZoneAnalysisService` para cada lado y selecciona vecinos únicamente entre las entidades incluidas por cada radio.
- `admin/GeoEntityManagementService`: crear/editar/eliminar entidades GEOINSIGHT con lista blanca y tipos derivados de los datasets; exige un atributo descriptivo real mínimo por dominio como regla propia de nuevas entidades, sin alterar la nulabilidad histórica SGC; valida geometría por dominio y protege entidades SGC.

### Infraestructura (`infrastructure/`)

- `bootstrap/DatasetBootstrapService`: verifica en cada arranque que los 5 datasets locales existan, puedan cargarse y coincidan con los conteos oficiales versionados; si alguno falla, invoca el descargador; si la descarga no se recupera, arranca con indicador de datos ausentes (FR-020). La validación local no vuelve a consultar la API oficial.
- `persistence/GeoJsonDatasetRepository`: lee y parsea los 5 GeoJSON (Jackson), los expone como `List<GeoscienceEntity>` por dominio e indica archivos ausentes, ilegibles o sin entidades.
- `persistence/JsonGeoEntityRepository`: CRUD de entidades GEOINSIGHT sobre `data/geoentities.json`.
- `persistence/JsonUserAccountRepository`: CRUD de cuentas sobre `data/users.json`; `AdminAccountSeeder` siembra el admin desde `config/admin-account.json` si no existe.
- `download/SgcDatasetDownloader`: descarga vía `java.net.http.HttpClient` con paginación (resultOffset/resultRecordCount, outSR=4326), replicando la lógica verificada de `scripts/download-datasets.ps1`.
- `BootstrapRunner`: orquesta `DatasetBootstrapService` + seeder al arrancar.

### Web (`web/`)

- Controladores REST (contratos en `contracts/api.md`):
  - `AuthController`: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`.
  - `LayerController`: `GET /api/layers`, `GET /api/layers/{domain}/geojson`, `GET /api/entities/{domain}?filtro=...`, `GET /api/entities/{domain}/{id}`.
  - `AnalysisController`: `POST /api/context`, `POST /api/zones/analyze`, `POST /api/zones/compare`.
  - `AdminController` (rol ADMIN): `GET /api/admin/entities`, `POST /api/admin/entities`, `PUT /api/admin/entities/{id}`, `DELETE /api/admin/entities/{id}`.
- Seguridad: Spring Security, sesión HTTP, login JSON, rutas `/api/admin/**` restringidas a rol ADMIN; la pantalla y recursos visuales del acceso son públicos, mientras la aplicación y el mapa exigen sesión (SC-008).
- DTOs de entrada validados (`@Valid`) y DTOs de salida inmutables.

### Frontend (`src/main/resources/static/`)

- `login.html`: identidad visual local, inicio de sesión y registro (rol usuario), sin SSO.
- `index.html`: shell autenticado sin búsqueda global ni controles duplicados, navegación por módulos, mapa Leaflet, control flotante de capas, paneles de filtros/contexto/zona/comparación/ayuda con acciones «Borrar» propias de cada análisis y administración solo para ADMIN.
- `js/map.js`: capas inicialmente inactivas; control flotante de capas en la esquina inferior derecha con icono convencional y selector desplegable; herramienta lateral “Explorar mapa” eliminada; panel contextual inicialmente colapsado; puntos densos en Canvas; protección contra respuestas asíncronas obsoletas; selección de entidades; vista previa de entidad al pasar el cursor (tooltip, incluidos los puntos en canvas); tabla de filtros con enfoque; selectores explícitos de coordenadas; centros/radios; dibujo administrativo Point/LineString/Polygon.
- `js/compare.js`: tarjetas por dominio, vecinos/distancias limitados al radio, estados explícitos, observaciones descriptivas cerradas y tabla resumen secundaria.
- `js/context.js`: consulta del contexto de una coordenada presentada en tres secciones (Resultado, Contexto geológico, Elementos cercanos) con tarjetas compactas, nombres descriptivos priorizados, distancias m/km con un decimal y estados de ausencia legibles; delega el dibujo del contexto en `GeoInsightMap.drawContext`.
- `js/admin.js`: formulario dinámico desde metadata, coerción tipada, vocabularios categóricos, lista persistente “Mis entidades”, capa administrativa por color y modal propio de eliminación.
- `js/auth.js`: login y registro local; no incluye SSO institucional.
- `css/styles.css`, `css/layers.css` y `css/context.css`: sistema visual general, control flotante de capas (esquina inferior derecha) y tarjetas/marcador de la consulta por coordenada.
- `lib/leaflet/`: Leaflet vendoreado localmente (offline).

### Performance (SC-001..SC-003)

- Carga en memoria al arranque y recorrido lineal por dominio. Con el volumen actual (~19 mil entidades), esta solución cumple SC-002/SC-003 sin introducir índices espaciales innecesarios.
- **Capas pesadas**: se sirve GeoJSON de visualización. La capa de unidades (>100 MB) usa una **representación simplificada solo para visualización** (Douglas-Peucker) mientras el análisis siempre usa la geometría completa. Decisión verificada en la tarea de frontend (SC-001).
- Los círculos de zona se dibujan en el frontend; los indicadores se calculan en el backend.
- Los puntos masivos se dibujan en un único canvas por capa para evitar miles de nodos SVG; la selección conserva hit-testing por feature.
- La UI administrativa dibuja una representación GeoJSON, pero el backend continúa siendo responsable de validar tipo de geometría, campos y tipos escalares.

## Project Structure

### Documentation (this feature)

```text
specs/001-geoinsight-core/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-dictionary.md   # Diccionario formal derivado de los GeoJSON completos
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── design.md            # Diagramas UML/Mermaid y decisiones POO
├── traceability.md      # Matriz datos → requisitos → código → pruebas
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── api.md           # REST API contracts
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
pom.xml
mvnw / mvnw.cmd / .mvn/wrapper/        # Maven Wrapper
scripts/
└── download-datasets.ps1              # descarga manual (reutilizado por el bootstrap)
config/
└── admin-account.json                 # cuenta admin sembrada (configuración, en git)
data/                                  # escribible, gitignored
├── users.json
└── geoentities.json
src/main/java/co/edu/distrital/geoinsight/
├── domain/
│   ├── geometry/                      # Coordinate, Geometry, Point, LineString, Polygon, Multi*, GeometryFactory
│   ├── model/                         # Domain, Origin, GeoscienceEntity, Zone, UserAccount, Role
│   └── repository/                    # DatasetRepository, GeoEntityRepository, UserAccountRepository
├── application/
│   ├── auth/                          # AuthenticationService, RegistrationService
│   ├── exploration/                   # LayerExplorationService
│   ├── analysis/                      # CoordinateContextService, ZoneAnalysisService, ZoneComparisonService
│   ├── admin/                         # GeoEntityManagementService
│   └── bootstrap/                     # DatasetBootstrapService
├── infrastructure/
│   ├── persistence/                   # GeoJsonDatasetRepository, JsonGeoEntityRepository, JsonUserAccountRepository, AdminAccountSeeder
│   ├── download/                      # SgcDatasetDownloader
│   └── bootstrap/                     # BootstrapRunner
└── web/
    ├── security/                      # SecurityConfig
    ├── controller/                    # AuthController, LayerController, AnalysisController, AdminController
    └── dto/                           # request/response DTOs
src/main/resources/
├── static/                            # login.html, index.html, css/, js/, lib/leaflet/
└── application.properties
src/test/java/co/edu/distrital/geoinsight/
├── domain/                            # geometría (distancia, contención), invariantes, reglas
├── application/                       # casos de uso con fakes de repositorio
└── infrastructure/                    # parsing GeoJSON, persistencia JSON
```

**Structure Decision**: un solo artefacto Maven con paquetes por capa (`domain`, `application`, `infrastructure`, `web`) y frontend estático servido por Spring Boot. Es la opción más simple que respeta las fronteras de la constitución (dominio independiente, persistencia detrás de repositorios, controladores solo HTTP) sin multi-módulos ni frameworks de frontend.

## Complexity Tracking

### Disponibilidad durante el arranque

`BootstrapRunner` participa en la inicialización de singletons del contexto,
antes de que el ciclo de vida de Tomcat abra el puerto. Ejecuta la siembra del
administrador y el bootstrap completo de datasets. Esto evita una ventana en
la que la API responda con todas las capas temporalmente no disponibles.
Cada descarga admite un máximo configurable de intentos y una espera base
configurable; la espera aumenta con el número de intento. Al agotarse, el
arranque continúa con `dataAvailable=false`, preservando FR-020 sin bloquear
indefinidamente el uso local.

### Integridad de datasets

El bootstrap compara cada archivo local con el conteo oficial versionado. Si
falta, está corrupto o incompleto, se marca como no disponible y se invoca el
descargador. Esta regla evita reutilizar archivos truncados y alinea el arranque
con la spec.

> **Fill ONLY if Constitution Check has violations that must be justified**

Sin violaciones justificadas. El único aumento deliberado de complejidad es la **jerarquía de geometría** (6 tipos) y **repositorios como interfaz**, ambos exigidos por el dominio (geometrías multiparte reales en los datasets) y por la constitución (persistencia detrás de abstracciones), no especulativos.
