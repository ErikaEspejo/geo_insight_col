# Diseño orientado a objetos

**Feature**: `001-geoinsight-core` | **Fuente**: [spec.md](./spec.md) | **Modelo detallado**: [data-model.md](./data-model.md)

Este documento hace explícito el diseño conceptual que guía la implementación. Las flechas de dependencia apuntan hacia el dominio: el dominio no conoce Spring, Jackson, HTTP, JSON ni Leaflet.

## Diagrama de clases del núcleo

```mermaid
classDiagram
    direction LR

    class Geometry {
        <<abstract>>
        +geoJsonType() String
        +distanceMeters(Coordinate) double
        +contains(Coordinate) boolean
        +bounds() Bounds
    }
    class Point
    class LineString
    class Polygon
    class MultiPoint
    class MultiLineString
    class MultiPolygon
    Geometry <|-- Point
    Geometry <|-- LineString
    Geometry <|-- Polygon
    Geometry <|-- MultiPoint
    Geometry <|-- MultiLineString
    Geometry <|-- MultiPolygon

    class Coordinate {
        <<record>>
        +lon double
        +lat double
        +distanceMeters(Coordinate) double
    }
    class Bounds { <<record>> }
    Geometry ..> Coordinate
    Geometry ..> Bounds
    Point *-- Coordinate
    LineString *-- "2..*" Coordinate
    Polygon *-- "anillo exterior" Coordinate
    Polygon *-- "0..* agujeros" Coordinate
    MultiPoint *-- "1..*" Point
    MultiLineString *-- "1..*" LineString
    MultiPolygon *-- "1..*" Polygon

    class GeoscienceEntity {
        -id String
        -domain Domain
        -origin Origin
        -geometry Geometry
        -attributes Map~String,Object~
        +attribute(String) Object
        +attributeString(String) Optional~String~
    }
    class Domain { <<enumeration>> }
    class Origin { <<enumeration>> }
    GeoscienceEntity *-- Domain
    GeoscienceEntity *-- Origin
    GeoscienceEntity *-- Geometry

    class Zone {
        <<record>>
        +center Coordinate
        +radiusMeters double
    }
    Zone *-- Coordinate

    class DatasetRepository {
        <<interface>>
        +findSgcByDomain(Domain) List
        +attributeNames(Domain) Set
        +distinctValues(Domain,String) List
        +isDatasetLoaded(Domain) boolean
    }
    class GeoEntityRepository {
        <<interface>>
        +findByDomain(Domain) List
        +findById(String) Optional
        +save(GeoscienceEntity) GeoscienceEntity
        +delete(String)
    }
    class UserAccountRepository { <<interface>> }
    class CountryBoundary { <<interface>> }

    class EntityCatalog
    class LayerExplorationService
    class CoordinateContextService
    class ZoneAnalysisService
    class ZoneComparisonService
    class GeoEntityManagementService
    EntityCatalog --> DatasetRepository
    EntityCatalog --> GeoEntityRepository
    LayerExplorationService --> EntityCatalog
    LayerExplorationService --> DatasetRepository
    CoordinateContextService --> EntityCatalog
    CoordinateContextService --> CountryBoundary
    ZoneAnalysisService --> EntityCatalog
    ZoneComparisonService --> ZoneAnalysisService
    GeoEntityManagementService --> DatasetRepository
    GeoEntityManagementService --> GeoEntityRepository

    class GeoJsonDatasetRepository
    class JsonGeoEntityRepository
    class JsonUserAccountRepository
    class GeoJsonCountryBoundary
    DatasetRepository <|.. GeoJsonDatasetRepository
    GeoEntityRepository <|.. JsonGeoEntityRepository
    UserAccountRepository <|.. JsonUserAccountRepository
    CountryBoundary <|.. GeoJsonCountryBoundary
```

## Decisiones POO demostradas

| Principio | Evidencia de diseño |
|---|---|
| Abstracción | `Geometry` define el contrato geoespacial común y oculta el algoritmo concreto de cada tipo. |
| Herencia | Los seis tipos GeoJSON admitidos especializan `Geometry`. |
| Polimorfismo | Los casos de uso invocan `distanceMeters`, `contains` y `bounds` mediante `Geometry`, sin condicionar por cada dominio. |
| Encapsulamiento | Entidades y geometrías protegen estado privado, validan al construirse y exponen colecciones inmutables; no requieren setters. |
| Composición | Una entidad se compone de dominio, origen, geometría y atributos; las variantes multiparte se componen de geometrías simples. |
| Inversión de dependencias | Los casos de uso dependen de interfaces del dominio; JSON y GeoJSON son adaptadores de infraestructura. |

No existen cinco subclases de `GeoscienceEntity` porque los datasets muestran el mismo comportamiento y difieren en geometría y atributos. Crear esas subclases produciría una jerarquía artificial; la especialización polimórfica legítima está en las geometrías.

## Fronteras arquitectónicas

```mermaid
flowchart LR
    Web[Web / REST / Leaflet] --> Application[Casos de uso]
    Infrastructure[JSON, GeoJSON, descarga SGC] --> Domain[Dominio]
    Application --> Domain
    Web --> Application
    Infrastructure -. implementa interfaces .-> Domain
```

