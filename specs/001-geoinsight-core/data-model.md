# Data Model: Sistema Núcleo de GeoInsight Colombia

**Feature**: `001-geoinsight-core` | **Date**: 2026-08-15 | **Plan**: [plan.md](./plan.md)

Modelo de dominio derivado de `spec.md` y de los datasets reales (ver `research.md` §2).

## Entidades

### `GeoscienceEntity`
Concepto común de los cinco dominios (spec, Key Entities).

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `String` | Identificador estable (`SGC-{domain}-{objectId}` o `GEO-{uuid}`). |
| `domain` | `enum Domain` | Inmutable. Uno de los cinco dominios. |
| `origin` | `enum Origin` | Inmutable. `SGC` o `GEOINSIGHT`. Invariante FR-003: si `SGC` → solo lectura; si `GEOINSIGHT` → editable/eliminable solo por admin (FR-005). |
| `geometry` | `Geometry` | Geometría según dominio; acepta variantes multiparte reales. |
| `attributes` | `Map<String,Object>` | Atributos reales del dataset (SGC) o provistos (GEOINSIGHT). Conserva claves originales (`Tipo`, `NombreFalla`, `Edad`, ...). |

### `Domain` (enum)
`MOVIMIENTO_EN_MASA`, `FALLA_GEOLOGICA`, `UNIDAD_GEOLOGICA`, `DOMINIO_TECTONICO`, `VOLCAN`.

La metadata de cada dominio expone: nombre legible, geometría admitida (punto/línea/polígono), atributos obligatorios para creación (derivados de datasets), atributos filtrables con sus dominios de valores (derivados en arranque).

### Metadatos de atributos administrativos

- `AttributeValueType`: enum `TEXT`, `INTEGER`, `DECIMAL`, `BOOLEAN`, derivado de valores escalares no nulos observados en cada GeoJSON.
- `editableAttributes`: lista blanca de campos descriptivos presentes y con tipo observable. Excluye metadatos técnicos del proveedor (`OBJECTID`, `GlobalID`, `Shape__*`, `ESRI_OID`, etc.).
- `editableAttributeTypes`: relación atributo → `AttributeValueType`, usada tanto para construir controles HTML como para validar el caso de uso.
- Un campo presente pero completamente nulo no se ofrece para captura porque su tipo no puede inferirse sin inventarlo.

### `Origin` (enum)
`SGC`, `GEOINSIGHT`. Siempre visible en consultas (FR-015).

### Geometría (jerarquía OOP)
| Clase | Representa |
|---|---|
| `Geometry` (abstracta) | `distanceTo(Coordinate)`, `contains(Coordinate)`, `bounds()` |
| `Point` | Un par lon/lat |
| `LineString` | Secuencia de puntos |
| `Polygon` | Anillo exterior (+ agujeros si presentes en dataset) |
| `MultiPoint` / `MultiLineString` / `MultiPolygon` | Variantes multiparte reales |

`Coordinate` (record): `lon` en [-180,180], `lat` en [-90,90]; `distanceTo(Coordinate)` haversine en metros.
`GeometryFactory`: construcción validada (deserializa desde el GeoJSON real).

### `Zone` (record)
`Coordinate centro` + `double radioMetros` (validación FR-016: radio > 0, centro válido).

### `UserAccount` y `Role`
| Campo | Reglas |
|---|---|
| `username` | Único; rechazo de duplicado en registro. |
| `passwordHash` | Hash BCrypt; nunca texto plano (FR-024). |
| `role` | `enum Role { USER, ADMIN }`. **Invariante**: una cuenta registrada por el usuario obtiene siempre `USER`; `ADMIN` solo proviene de la cuenta sembrada (FR-022, FR-023). |

## Resultados de análisis (DTOs de dominio → web)

- `CoordinateContextResult`: por dominio, resultado o ausencia explícita (FR-014):
  - Unidades contenientes (0..n), Dominios contenientes (0..n),
  - Falla más cercana + distancia (m), Movimiento más cercano + distancia (m), Volcán más cercano + distancia (m).
- `ZoneBreakdown`: `dataAvailable`, conteo, distribución (`byTipo` por `TIPO` para movimientos — además de `bySubtipo`/`byClasMapa` — y por `Tipo` para fallas, `Edad` para unidades geológicas y `NombreDT` para dominios tectónicos), entidades intersectadas y `Sin clasificar` para registros sin el atributo. `dataAvailable=false` no equivale a conteo cero.
- `ZoneIndicators`: cinco `ZoneBreakdown` y la zona consultada. Solo descriptivos (FR-012).
- `ComparedZone`: composición de `ZoneIndicators` + `CoordinateContextResult` para el mismo centro; reutiliza reglas existentes, no duplica geometría.
- `ZoneComparison`: `ComparedZone` de A y B, mismo esquema lado a lado (SC-004).

## Reglas de negocio (trazables)

| Regla | Fuente |
|---|---|
| SGC solo lectura | FR-003 |
| Solo admin crea/edita/elimina GEOINSIGHT | FR-004, FR-005 |
| Solo atributos reales en filtros | FR-008 |
| Ausencia explícita, sin inventar | FR-014, FR-001 (SC-007) |
| Procedencia visible | FR-015 |
| Validación de coordenadas/radios | FR-016 |
| Respetar tipo/CRS/orden de coordenadas | FR-017 |
| Campos obligatorios por dominio desde datasets | FR-018 |
| Lista blanca y tipos administrativos desde datasets | FR-032, SC-009 |
| Filtros OR dentro del atributo y AND entre atributos | FR-027 |
| Dataset ausente distinto de resultado vacío | FR-020, FR-031 |
| GEOINSIGHT participa en consultas sin pasar por SGC | FR-019 |
| Sin riesgo/amenaza/predicción | FR-012, SC-005 |
| Autenticación obligatoria | FR-021, SC-008 |
| Registro nunca admin; admin sembrado | FR-022, FR-023 |
| Hash de contraseñas | FR-024 |
| Restricción admin por rol | FR-025 |

## Persistencia

| Dato | Archivo | Rol |
|---|---|---|
| Datasets SGC | `docs/datasets/*.geojson` | Solo lectura, auto-descarga en bootstrap |
| Entidades GEOINSIGHT | `data/geoentities.json` | Escribible por admin |
| Cuentas | `data/users.json` | Escribible (registro/seeder) |
| Cuenta admin sembrada | `config/admin-account.json` | Configuración, en git |
