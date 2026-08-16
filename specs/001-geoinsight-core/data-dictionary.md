# Diccionario formal de datos SGC

**Fecha de perfilado**: 2026-08-16 | **CRS de salida**: EPSG:4326 | **Fuente**: cinco GeoJSON completos descargados de las APIs REST del SGC.

Este diccionario fue derivado programáticamente de todos los registros locales, no de muestras. `Nulos` cuenta valores JSON `null`; `Blancos` cuenta cadenas vacías o compuestas solo por espacios. Los tipos corresponden a valores observados después del parseo JSON. La geometría está en `Feature.geometry`, no en `properties`.

## Convenciones y restricciones comunes

- Coordenadas en orden longitud/latitud; longitud `[-180, 180]` y latitud `[-90, 90]`.
- Los registros SGC son de solo lectura. La nulabilidad observada se conserva sin normalización.
- `Técnico` identifica metadatos o medidas del proveedor: se conservan al consultar, pero no se capturan manualmente.
- `Descriptivo` identifica información de dominio que puede exponerse para captura si tiene tipo observable.
- `Requerido GEO` solo aplica a nuevas entidades `GEOINSIGHT`; no reinterpreta registros históricos SGC.
- Un campo `SIN TIPO` está presente, pero todos sus valores son nulos; no se ofrece para captura.

## Fallas geológicas

Archivo `Fallas.geojson`: 4.866 registros; geometría principal `LineString`, con soporte de `MultiLineString`.

| Campo | Tipo observado | Nulos | Blancos | Distintos | Clasificación | Restricción/uso |
|---|---:|---:|---:|---:|---|---|
| `OBJECTID` | INTEGER | 0 | 0 | 4.866 | Técnico | Identificador del proveedor; no editable. |
| `Tipo` | TEXT | 0 | 0 | 16 | Descriptivo | Filtrable, editable y categórico. |
| `NombreFalla` | TEXT | 2.816 | 9 | 555 | Descriptivo | Filtrable y editable; requerido y no blanco solo para GEOINSIGHT. |
| `Comentarios` | SIN TIPO | 4.866 | 0 | 0 | Descriptivo sin evidencia | No capturable: no existe tipo observable. |
| `GlobalID` | TEXT | 0 | 0 | 4.866 | Técnico | Identificador global del proveedor; no editable. |
| `Shape__Length` | DECIMAL | 0 | 0 | 4.866 | Técnico | Medida derivada del proveedor; no editable. |

## Movimientos en masa

Archivo `Inventario_de_movimientos_en_masa.geojson`: 6.826 registros; geometría `Point`, con soporte de `MultiPoint`.

| Campo | Tipo observado | Nulos | Blancos | Distintos | Clasificación | Restricción/uso |
|---|---:|---:|---:|---:|---|---|
| `FID` | INTEGER | 0 | 0 | 6.826 | Técnico | Identificador del proveedor; no editable. |
| `OBJECTID` | DECIMAL | 0 | 0 | 4 | Técnico | Metadato del proveedor; no editable. |
| `ID` | INTEGER | 0 | 0 | 6.826 | Descriptivo | Editable; valor entero. |
| `INV_MOVIMI` | INTEGER | 0 | 0 | 6.826 | Descriptivo | Editable; valor entero. |
| `F35DOV_TIP` | INTEGER | 0 | 0 | 7 | Técnico/codificado | Se conserva; no se captura porque `TIPO` aporta la clasificación descriptiva. |
| `TIPO` | TEXT | 0 | 0 | 7 | Descriptivo | Filtrable, editable, categórico y requerido para GEOINSIGHT. |
| `SUBTIPO_MO` | INTEGER | 0 | 0 | 24 | Técnico/codificado | Se conserva; no editable. |
| `SUBTIPO` | TEXT | 0 | 0 | 24 | Descriptivo | Filtrable, editable y categórico. |
| `CLAS_MAPA` | TEXT | 0 | 0 | 10 | Descriptivo | Filtrable, editable y categórico. |
| `ETIQUETA_M` | TEXT | 0 | 0 | 23 | Descriptivo | Editable. |
| `ESRI_OID` | INTEGER | 0 | 0 | 6.826 | Técnico | Identificador ESRI; no editable. |

## Unidades geológicas

Archivo `Mapa_Geologico_de_Colombia_2015.geojson`: 7.461 registros; geometría principal `Polygon`, con soporte de `MultiPolygon` y agujeros.

| Campo | Tipo observado | Nulos | Blancos | Distintos | Clasificación | Restricción/uso |
|---|---:|---:|---:|---:|---|---|
| `OBJECTID` | INTEGER | 0 | 0 | 7.461 | Técnico | Identificador del proveedor; no editable. |
| `SimboloUC` | TEXT | 0 | 0 | 188 | Descriptivo | Filtrable, editable y requerido para GEOINSIGHT. |
| `N°CartaColores` | SIN TIPO | 7.461 | 0 | 0 | Descriptivo sin evidencia | No capturable: no existe tipo observable. |
| `Descripcion` | TEXT | 2 | 0 | 178 | Descriptivo | Editable. |
| `Edad` | TEXT | 2 | 0 | 91 | Descriptivo | Filtrable, editable y categórico. |
| `UGIntegradas` | TEXT | 4.580 | 2 | 482 | Descriptivo | Editable; admite ausencia. |
| `Comentarios` | TEXT | 7.459 | 0 | 1 | Descriptivo | Editable; admite ausencia. |
| `GlobalID` | TEXT | 0 | 0 | 7.461 | Técnico | Identificador global del proveedor; no editable. |
| `Shape__Area` | DECIMAL | 0 | 0 | 7.461 | Técnico | Medida derivada; no editable. |
| `Shape__Length` | DECIMAL | 0 | 0 | 7.461 | Técnico | Medida derivada; no editable. |

## Dominios tectónicos

Archivo `Mapa_Tectonico_de_Colombia_2017.geojson`: 3 registros; geometría principal `Polygon`, con soporte de `MultiPolygon` y agujeros.

| Campo | Tipo observado | Nulos | Blancos | Distintos | Clasificación | Restricción/uso |
|---|---:|---:|---:|---:|---|---|
| `FID` | INTEGER | 0 | 0 | 3 | Técnico | Identificador del proveedor; no editable. |
| `OBJECTID` | INTEGER | 0 | 0 | 3 | Técnico | Identificador del proveedor; no editable. |
| `CodigoDT` | TEXT | 0 | 0 | 3 | Descriptivo | Editable. |
| `NombreDT` | TEXT | 0 | 0 | 3 | Descriptivo | Filtrable, editable y requerido para GEOINSIGHT. |
| `AreaDT` | DECIMAL | 0 | 0 | 3 | Técnico/medida | Medida de fuente; no editable. |
| `SHAPE_Leng` | DECIMAL | 0 | 0 | 3 | Técnico | Medida derivada; no editable. |
| `SHAPE_Area` | DECIMAL | 0 | 0 | 3 | Técnico | Medida derivada; no editable. |
| `Label` | TEXT | 0 | 0 | 3 | Descriptivo | Editable. |
| `Shape__Area` | DECIMAL | 0 | 0 | 3 | Técnico | Medida derivada; no editable. |
| `Shape__Length` | DECIMAL | 0 | 0 | 3 | Técnico | Medida derivada; no editable. |

## Volcanes

Archivo `Volcanes.geojson`: 61 registros; geometría `Point`, con soporte de `MultiPoint`.

| Campo | Tipo observado | Nulos | Blancos | Distintos | Clasificación | Restricción/uso |
|---|---:|---:|---:|---:|---|---|
| `OBJECTID` | INTEGER | 0 | 0 | 61 | Técnico | Identificador del proveedor; no editable. |
| `VolcanID` | SIN TIPO | 61 | 0 | 0 | Descriptivo sin evidencia | No capturable: no existe tipo observable. |
| `NombreVolcan` | TEXT | 0 | 15 | 46 | Descriptivo | Filtrable y editable; requerido y no blanco para GEOINSIGHT. Los 15 blancos son 14 cadenas vacías y 1 espacio. |
| `AlturaSobreNivelMar` | INTEGER | 4 | 0 | 38 | Descriptivo/medida | Editable como entero; admite ausencia. |
| `Latitud` | TEXT | 0 | 0 | 61 | Técnico redundante | Se conserva; la posición válida proviene de la geometría GeoJSON. |
| `Longitud` | TEXT | 0 | 0 | 61 | Técnico redundante | Se conserva; la posición válida proviene de la geometría GeoJSON. |
| `Comentarios` | TEXT | 53 | 1 | 3 | Descriptivo | Editable; admite ausencia. |
| `URL` | TEXT | 41 | 0 | 19 | Descriptivo | Editable como texto; no se infieren reglas de URL no presentes en la spec. |
| `GlobalID` | TEXT | 0 | 0 | 61 | Técnico | Identificador global del proveedor; no editable. |

## Contrato de creación GEOINSIGHT

| Dominio | Geometría principal | Campo requerido | Campos filtrables |
|---|---|---|---|
| Movimiento en masa | Point | `TIPO` | `TIPO`, `SUBTIPO`, `CLAS_MAPA` |
| Falla geológica | LineString | `NombreFalla` | `NombreFalla`, `Tipo` |
| Unidad geológica | Polygon | `SimboloUC` | `SimboloUC`, `Edad` |
| Dominio tectónico | Polygon | `NombreDT` | `NombreDT` |
| Volcán | Point | `NombreVolcan` | `NombreVolcan` |

Los campos editables finales son la intersección entre la lista descriptiva aprobada, los campos presentes y los campos con tipo observable. Esta regla está implementada por `DomainCatalogs` y `GeoJsonDatasetRepository`; el backend vuelve a validarla aunque la solicitud no provenga de la interfaz web.

