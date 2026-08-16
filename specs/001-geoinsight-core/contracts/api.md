# REST API Contracts: Sistema Núcleo de GeoInsight Colombia

**Feature**: `001-geoinsight-core` | **Date**: 2026-08-15 | **Base URL**: `http://localhost:8080`

Formato JSON. Errores: `{ "message": "..." }` con el código HTTP correspondiente (400 validación, 401 no autenticado, 403 sin permiso, 404 no encontrado, 409 conflicto).

## Alcance del contrato

Este documento distingue dos superficies HTTP diferentes:

- **rutas del frontend:** páginas y recursos estáticos que el navegador solicita para presentar la interfaz;
- **endpoints REST del backend:** operaciones bajo `/api/**` que reciben o devuelven JSON o GeoJSON.

Las rutas del frontend no constituyen una API y los endpoints del backend no representan pantallas navegables.

## Rutas del frontend

| Método y ruta | Recurso | Acceso |
|---|---|---|
| `GET /login.html` | pantalla de inicio de sesión y registro | público |
| `GET /` | entrada principal de la aplicación | autenticado |
| `GET /index.html` | interfaz cartográfica principal | autenticado |
| `GET /css/styles.css` | estilos mínimos compartidos | público |
| `GET /css/login*.css` | estilos de la pantalla de acceso | público |
| `GET /js/api.js` | cliente HTTP usado durante el acceso | público |
| `GET /js/auth.js` | comportamiento de login y registro | público |
| `GET /images/geoinsight-logo.png` | identidad visual del acceso | público |
| `GET /images/colombian-volcanic-landscape.png` | imagen del acceso | público |
| demás recursos `/css/**`, `/js/**`, `/images/**`, `/assets/**` y `/lib/**` | interfaz completa y Leaflet local | autenticado |

Cuando un navegador solicita una ruta protegida sin sesión y acepta HTML, Spring Security lo redirige a `/login.html`.

## Endpoints REST del backend

### Disponibilidad del servidor

El puerto HTTP solo se abre después de completar la verificación, las descargas
con reintentos y la carga inicial de datasets (FR-050, FR-051). Por tanto, una
conexión aceptada nunca observa el estado transitorio del bootstrap. Si un
dataset continúa ausente después de agotar los intentos, `GET /api/layers`
informa `dataAvailable=false` para ese dominio (FR-020).

Los endpoints protegidos responden `401` en formato JSON cuando no existe sesión. Las rutas administrativas responden `403` cuando la sesión pertenece a un usuario sin rol `ADMIN`.

### Autenticación

#### `POST /api/auth/register`

Los recursos estáticos necesarios para presentar `login.html` son la única
excepción a la autenticación. `/api/basemap/**` exige sesión y responde `401`
cuando no existe una sesión autenticada.

Registra una cuenta de consulta. **Siempre** crea rol `USER` (FR-022).

```json
{ "username": "ana", "password": "secreta123" }
```
- `201 Created` → `{ "username": "ana", "role": "USER", "admin": false }`
- `400` username/password inválidos (longitud mínima)
- `409` username ya existe

#### `POST /api/auth/login`
```json
{ "username": "ana", "password": "secreta123" }
```
- `200` → `{ "username": "ana", "role": "USER", "admin": false }`
- `401` credenciales incorrectas

#### `POST /api/auth/logout`
- `204 No Content`

#### `GET /api/auth/me`
- `200` → `{ "username": "...", "role": "USER"|"ADMIN", "admin": bool }`
- `401` sin sesión

### Exploración

### `GET /api/layers`
Metadatos de los cinco dominios para construir el control de capas y filtros (FR-008, FR-018).

```json
[
  {
      "domain": "VOLCAN",
      "name": "Volcanes",
      "geometryType": "Point",
      "count": 61,
      "dataAvailable": true,
      "requiredAttributes": ["NombreVolcan"],
      "filterableAttributes": [
        { "name": "NombreVolcan", "values": ["Nevado del Ruiz", "..."] }
      ],
      "editableAttributes": ["NombreVolcan", "AlturaSobreNivelMar", "Comentarios", "URL"],
      "editableAttributeTypes": {"NombreVolcan":"TEXT", "AlturaSobreNivelMar":"INTEGER", "Comentarios":"TEXT", "URL":"TEXT"}
  }
]
```
  `values` contiene todos los valores distintos observados para cada atributo
  filtrable y `editableAttributes` deriva de los datasets reales; atributos
  inexistentes no aparecen. Los campos técnicos del proveedor (identificadores
  internos y medidas de geometría) no se ofrecen para captura manual.
  `dataAvailable=false` indica inequívocamente dataset ausente o sin datos
  cargados para ese dominio (FR-020). Las capas de polígonos pesadas se sirven
  con geometría simplificada para visualización (SC-001); el análisis usa
  siempre la geometría completa.

### `GET /api/layers/{domain}/geojson`
GeoJSON `FeatureCollection` de las entidades del dominio (SGC + GEOINSIGHT), para render Leaflet. `domain` ∈ {`MOVIMIENTO_EN_MASA`,`FALLA_GEOLOGICA`,`UNIDAD_GEOLOGICA`,`DOMINIO_TECTONICO`,`VOLCAN`}. Cada feature incluye `properties.origin` (procedencia).

### `GET /api/entities/{domain}?attr=valor&attr=otro&attr2=valor`
Entidades filtradas por atributos reales (GeoJSON). Valores repetidos del mismo
atributo se combinan con OR; atributos distintos se combinan con AND. Filtro
con atributo inexistente → `400`.

### `GET /api/entities/{domain}/{id}`
- `200` → detalle:
```json
{
  "id": "SGC-UNIDAD_GEOLOGICA-123",
  "domain": "UNIDAD_GEOLOGICA",
  "origin": "SGC",
  "attributes": { "SimboloUC": "Q1", "Edad": "...", "Descripcion": "..." },
  "geometry": { "type": "Polygon", "coordinates": [...] }
}
```
- `404` no existe

### Análisis

### `POST /api/context`
Contexto geocientífico de una coordenada (FR-009).

```json
{ "lon": -74.07, "lat": 4.71 }
```
- `200`:
```json
{
  "coordinate": { "lon": -74.07, "lat": 4.71 },
  "insideCoverage": true,
  "geologicalUnits": [
    {
      "id": "SGC-UNIDAD_GEOLOGICA-123",
      "domain": "UNIDAD_GEOLOGICA",
      "origin": "SGC",
      "geometry": { "type": "Polygon", "coordinates": [...] },
      "attributes": { "SimboloUC": "Q1", "Edad": "..." }
    }
  ],
  "tectonicDomains": [
    {
      "id": "SGC-DOMINIO_TECTONICO-1",
      "domain": "DOMINIO_TECTONICO",
      "origin": "SGC",
      "geometry": { "type": "Polygon", "coordinates": [...] },
      "attributes": { "NombreDT": "..." }
    }
  ],
  "nearestFault":   { "entity": {...}, "distanceMeters": 1234.5 } | null,
  "nearestMassMovement": { "entity": {...}, "distanceMeters": 9876.5 } | null,
  "nearestVolcano": { "entity": {...}, "distanceMeters": 54321.0 } | null
}
```
`null` = ausencia explícita por dominio (FR-014). Fuera de cobertura se responde
`200` con `insideCoverage=false`, ambas listas vacías y los tres vecinos en
`null`. La cobertura se selecciona por disponibilidad: dominios tectónicos,
luego unidades geológicas y finalmente el basemap. `400` coordenada inválida.

### `POST /api/zones/analyze`
Indicadores descriptivos de una zona (FR-010).

```json
{ "lon": -74.07, "lat": 4.71, "radiusMeters": 10000 }
```
- `200`:
```json
{
  "zone": { "lon": ..., "lat": ..., "radiusMeters": 10000 },
  "massMovements": { "dataAvailable": true, "count": 12, "byTipo": { "Deslizamiento": 8, "Caida": 4 }, "bySubtipo": {...}, "byClasMapa": {...}, "entities": [...] },
  "faults": { "dataAvailable": true, "count": 3, "byTipo": { "Falla": 2, "Lineamiento": 1 }, "entities": [...] },
  "geologicalUnits": { "dataAvailable": true, "count": 4, "byTipo": { "Cuaternario": 2, "Jurásico": 2 }, "entities": [...] },
  "tectonicDomains": { "dataAvailable": true, "count": 1, "byTipo": { "Basamento Amazónico": 1 }, "entities": [...] },
  "volcanoes": { "dataAvailable": true, "count": 0, "entities": [] }
}
```
`400` radio no válido. Sin conclusiones de riesgo (FR-012). Cada dominio
incluye su distribución según el atributo de clasificación del dataset:
`massMovements.byTipo` por `TIPO` (además de `bySubtipo` por `SUBTIPO` y
`byClasMapa` por `CLAS_MAPA`), `faults.byTipo` por `Tipo`,
`geologicalUnits.byTipo` por `Edad` y `tectonicDomains.byTipo` por `NombreDT`.
`volcanoes` no ofrece distribución porque el dataset no tiene atributo
categórico. Los registros con el atributo vacío o ausente se agrupan
explícitamente bajo la categoría `Sin clasificar`; así, la sumatoria de cada
distribución siempre equivale a `count`.

### `POST /api/zones/compare`
Comparación de dos zonas (FR-011).

```json
{
  "zoneA": { "lon": -74.07, "lat": 4.71 },
  "zoneB": { "lon": -75.0, "lat": 5.0 },
  "radiusMeters": 10000
}
```
- `radiusMeters` es único y se aplica por igual a las dos zonas; no se aceptan radios independientes.
- `200` → `{ "zoneA": {indicators}, "zoneB": {indicators} }`, mismo esquema en ambas (SC-004).
  Cada lado conserva los indicadores de zona e incluye `nearestFault`,
  `nearestMassMovement` y `nearestVolcano`, seleccionados únicamente entre las
  entidades incluidas por el radio. Cada desglose incluye `dataAvailable` para distinguir un
  conteo cero de un dataset no disponible (FR-014, FR-020). Las distancias son
  descriptivas y no expresan riesgo, amenaza ni seguridad (FR-012).

### Administración (rol ADMIN)

Todas responden `401` sin sesión y `403` para rol USER.

### `POST /api/admin/entities`
Crea una entidad GEOINSIGHT (FR-004, FR-018).

```json
{
  "domain": "VOLCAN",
  "geometry": { "type": "Point", "coordinates": [-74.0, 4.7] },
  "attributes": { "NombreVolcan": "Nuevo", "AlturaSobreNivelMar": 4300 }
}
```
- `201` → detalle con `origin: "GEOINSIGHT"`
- `400` campos obligatorios faltantes, atributo fuera de la lista permitida,
  tipo escalar incompatible o geometría no admitida. Un campo `INTEGER` debe
  viajar como número JSON, no como cadena numérica.

Los atributos obligatorios de las entidades nuevas GEOINSIGHT son `TIPO`,
`NombreFalla`, `SimboloUC`, `NombreDT` y `NombreVolcan` para sus respectivos
dominios. Esta regla garantiza identificación descriptiva mínima y no modifica
los valores nulos o vacíos existentes en registros SGC.

### `GET /api/admin/entities`
Lista únicamente entidades persistidas con origen `GEOINSIGHT`. La UI la usa
para mantener “Mis entidades” visible y dibujar una capa administrativa por
color de dominio. No mezcla registros SGC.

### `PUT /api/admin/entities/{id}`
Actualiza una entidad GEOINSIGHT (FR-005). Id de origen SGC → `403` (FR-003).
- `200` → detalle actualizado | `404` no existe | `403` SGC

### `DELETE /api/admin/entities/{id}`
Elimina una entidad GEOINSIGHT (FR-005). Id SGC → `403`.
- `204` | `404` no existe | `403` SGC

### Fondo vectorial offline

### `GET /api/basemap/colombia`
GeoJSON `FeatureCollection` con contorno de Colombia (+ departamentos si disponibles) para fondo local sin conexión.
