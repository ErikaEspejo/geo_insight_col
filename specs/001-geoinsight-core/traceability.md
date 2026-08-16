# Matriz de trazabilidad SDD

**Propósito**: demostrar la cadena verificable `datos → especificación → diseño → tarea → implementación → prueba`, independientemente de que los primeros artefactos hayan entrado al repositorio en un mismo commit.

## Secuencia metodológica reconstruida

1. **Fuente institucional**: APIs REST SGC y cinco GeoJSON completos, perfilados en [data-dictionary.md](./data-dictionary.md) y [research.md](./research.md).
2. **Especificación**: historias, escenarios Given/When/Then, FR-001..FR-051 y SC-001..SC-011 en [spec.md](./spec.md).
3. **Diseño**: modelo e invariantes en [data-model.md](./data-model.md), UML en [design.md](./design.md) y contratos HTTP en [contracts/api.md](./contracts/api.md).
4. **Planificación**: fronteras y decisiones técnicas en [plan.md](./plan.md); unidades ejecutables en [tasks.md](./tasks.md).
5. **Implementación y pruebas**: paquetes `domain`, `application`, `infrastructure`, `web` y suite JUnit.

El historial Git no se usa como sustituto de esta evidencia: el orden lógico queda establecido por las dependencias entre artefactos y por las referencias de tareas y pruebas a requisitos previamente identificados.

## Datos y decisiones de modelo

| Evidencia de datos | Decisión/requisito | Diseño | Implementación | Prueba |
|---|---|---|---|---|
| Geometrías Point/LineString/Polygon y variantes multiparte | FR-013, FR-017 | `Geometry` abstracta y seis especializaciones | `domain/geometry/*`, `GeoJsonGeometryParser` | `GeometryTest`, `GeoJsonDatasetRepositoryTest` |
| Cinco esquemas y atributos reales | FR-001, FR-008, FR-018, FR-032 | `GeoscienceEntity.attributes`, `DatasetRepository` | `DomainCatalogs`, `GeoJsonDatasetRepository` | `GeoJsonDatasetRepositoryTest`, `LayerExplorationServiceTest`, `GeoEntityManagementServiceTest` |
| Identificadores y medidas técnicas del proveedor | FR-032 | Lista blanca descriptiva | `editableAttributes`, `editableAttributeTypes` | `GeoEntityManagementServiceTest` |
| Nulos y blancos históricos | FR-014, FR-018, FR-046 | Ausencia explícita; requisito solo para GEOINSIGHT | validación administrativa y presentación | `GeoEntityManagementServiceTest`, `AnalysisWebTest` |
| Conteos oficiales 61/4.866/7.461/3/6.826 | FR-001, FR-020, FR-050 | bootstrap previo al puerto | `SgcDatasets`, `DatasetBootstrapService`, `BootstrapRunner` | `SgcDatasetDownloaderTest`, `PerformanceAcceptanceTest` |

## Requisitos funcionales

| Requisitos | Diseño/contrato | Implementación principal | Tareas | Evidencia de prueba |
|---|---|---|---|---|
| FR-001, FR-020 | Repositorio de datasets y `dataAvailable` | `GeoJsonDatasetRepository`, `DatasetBootstrapService`, `LayerController` | T012–T016 | `GeoJsonDatasetRepositoryTest`, `LayerWebTest` |
| FR-002, FR-003, FR-015, FR-019 | `Origin`, entidad inmutable, contratos de detalle | `GeoscienceEntity`, `EntityCatalog`, `GeoEntityManagementService` | T009, T025, T043 | `EntityAndZoneTest`, `GeoEntityManagementServiceTest`, `LayerWebTest` |
| FR-004, FR-005 | Contrato admin y repositorio GEOINSIGHT | `AdminController`, `GeoEntityManagementService`, `JsonGeoEntityRepository` | T042–T046 | `GeoEntityManagementServiceTest`, `AdminWebTest`, `JsonRepositoriesTest` |
| FR-006, FR-007, FR-026, FR-028 | Capas y selección | `LayerController`, `map.js`, `ui.js` | T024–T029 | `LayerWebTest`; quickstart E2 |
| FR-008, FR-027 | Metadatos y álgebra de filtros | `LayerExplorationService`, `DomainCatalogs`, `map.js` | T023–T029 | `LayerExplorationServiceTest`, `LayerWebTest`; quickstart E2 |
| FR-009, FR-013, FR-014, FR-017 | Contrato de contexto y geometrías polimórficas | `CoordinateContextService`, `Geometry` | T007–T011, T030–T032 | `CoordinateContextServiceTest`, `GeometryTest`, `AnalysisWebTest` |
| FR-010, FR-016 | Contrato de zona y `Zone` | `ZoneAnalysisService`, `ZoneRequest` | T033–T037 | `EntityAndZoneTest`, `ZoneAnalysisServiceTest`, `AnalysisWebTest` |
| FR-011, FR-031 | Comparación compuesta por dos análisis equivalentes | `ZoneComparisonService`, `ComparedZone` | T038–T041 | `ZoneComparisonServiceTest`, `AnalysisWebTest` |
| FR-012 | Resultados exclusivamente descriptivos | DTOs de análisis y textos cerrados | servicios de análisis, `zone.js`, `compare.js` | `ZoneBreakdownTest`; quickstart E4–E5 |
| FR-018, FR-032, FR-033 | Diccionario, lista blanca, tipos y geometría por dominio | `DatasetRepository`, contrato admin | `DomainCatalogs`, `GeoEntityManagementService`, `admin.js` | `GeoEntityManagementServiceTest`, `AdminWebTest` |
| FR-021, FR-025 | Seguridad por sesión y rol | contrato de autenticación | `SecurityConfig`, `AuthController` | `AuthWebTest`, `AdminWebTest`, `LayerWebTest` |
| FR-022, FR-023, FR-024 | Registro USER, admin sembrado, BCrypt | `UserAccount`, repositorio de usuarios | `RegistrationService`, `AdminAccountSeeder`, `JsonUserAccountRepository` | `RegistrationServiceTest`, `AuthWebTest`, `JsonRepositoriesTest` |
| FR-029, FR-030, FR-038 | Selección explícita y dibujo de zonas | contratos UI de contexto/zona/comparación | `map.js`, `context.js`, `zone.js`, `compare.js` | quickstart E3–E5 |
| FR-034 | Gestión y confirmación administrativa | contrato admin | `admin.js`, `AdminController` | `AdminWebTest`; quickstart E6 |
| FR-035, FR-037 | Navegación y ayuda por rol | diseño frontend | `index.html`, `ui.js` | quickstart E7 |
| FR-036 | Acceso local sin SSO y recursos públicos mínimos | contrato auth y seguridad | `login.html`, `auth.js`, `SecurityConfig` | `AuthWebTest`; quickstart E1 |
| FR-039 | Detalle adaptable | diseño frontend | `styles.css`, `ui.js` | quickstart E2 |
| FR-040, FR-041, FR-042 | Control flotante y panel contextual | diseño frontend | `map.js`, `layers.css`, `index.html` | quickstart E2 |
| FR-043, FR-044, FR-045, FR-046 | Presentación y resaltado del contexto | contrato de contexto | `context.js`, `map.js`, `context.css` | `AnalysisWebTest`; quickstart E3 |
| FR-047 | Vista previa de entidades | diseño de interacción | `map.js`, `zone.js`, `compare.js` | quickstart E2, E4, E5 |
| FR-048 | Limpieza aislada de resultados | diseño de interacción | `context.js`, `zone.js`, `compare.js`, `map.js` | quickstart E3–E5 |
| FR-049 | Cascada de cobertura | `CountryBoundary`, contrato de contexto | `CoordinateContextService`, `GeoJsonCountryBoundary` | `CoordinateContextServiceTest`, `AnalysisWebTest` |
| FR-050, FR-051 | Bootstrap bloqueante y reintentos finitos | plan de disponibilidad | `BootstrapRunner`, `DatasetBootstrapService`, `SgcDatasetDownloader` | `SgcDatasetDownloaderTest`, `LayerWebTest` |

## Criterios de éxito

| Criterio | Evidencia |
|---|---|
| SC-001, SC-002, SC-003 | `PerformanceAcceptanceTest` sobre los cinco datasets completos. |
| SC-004 | `ZoneComparisonServiceTest`, `AnalysisWebTest`, quickstart E5. |
| SC-005 | `ZoneBreakdownTest` y validación manual E4–E5. |
| SC-006 | `GeoEntityManagementServiceTest`, `AdminWebTest`, E6. |
| SC-007 | `LayerExplorationServiceTest`, `LayerWebTest`, E2. |
| SC-008 | `AuthWebTest`, `AdminWebTest`, `LayerWebTest`, E1. |
| SC-009 | `GeoEntityManagementServiceTest`, `AdminWebTest`, E6. |
| SC-010 | Validación manual E3–E6 registrada en [validation.md](./validation.md). |
| SC-011 | Pruebas de bootstrap y primera respuesta en `LayerWebTest`; diseño de `BootstrapRunner`. |

## Estado de evidencia

- Especificación, diseño, contratos, tareas, código y pruebas tienen vínculos explícitos.
- Las validaciones visuales E1–E9 fueron realizadas manualmente y están registradas en `validation.md`.
- La suite automatizada cubre dominio, aplicación, persistencia, seguridad, web y aceptación de rendimiento.

