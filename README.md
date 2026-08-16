# geo_insight_col

GeoInsight Colombia: sistema de información geocientífica que integra datos
abiertos del Servicio Geológico Colombiano (SGC) con entidades propias
(GEOINSIGHT). Mapa Leaflet con 5 capas, consulta por coordenada, análisis y
comparación de zonas, y gestión administrativa de entidades.

Desarrollo guiado por especificaciones (`specs/001-geoinsight-core/`): la
especificación, el plan, el contrato REST y el quickstart de validación son la
fuente de verdad del comportamiento esperado.

## Requisitos

- Java 21
- Maven 3.9.x o el Maven Wrapper incluido (`mvnw.cmd`)

## Compilar y ejecutar

```powershell
.\mvnw.cmd clean verify        # compila + ejecuta toda la suite de tests
.\mvnw.cmd spring-boot:run     # levanta el servidor en http://localhost:8080
```

Al arrancar, el backend completa el bootstrap **antes de abrir el puerto 8080**:

1. verifica localmente que los 5 datasets tengan sus conteos oficiales;
2. descarga los ausentes, corruptos o incompletos desde las APIs REST del SGC;
3. reintenta cada descarga hasta 3 veces, con espera incremental;
4. carga los datasets completos en memoria y siembra la cuenta admin;
5. solo entonces empieza a aceptar conexiones. Si un dataset sigue fallando
   tras los reintentos, arranca indicando `dataAvailable=false` para ese dominio.

Los reintentos se configuran en `application.properties` mediante
`geoinsight.download.max-attempts` y `geoinsight.download.retry-delay-ms`.

## Cuenta admin (sembrada)

- **Usuario**: `admin`
- **Contraseña**: `admin123`

Sembrada en `config/admin-account.json` (usuario + hash BCrypt). No puede
crearse por registro; el registro público siempre otorga rol `USER`. Las
operaciones de administración (`/api/admin/**`) exigen rol `ADMIN`.

## Datasets SGC

Los datasets **no están versionados en git** (~122 MB; se obtienen de la fuente
oficial). Descarga manual opcional (el bootstrap también los descarga):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\download-datasets.ps1
```

| Archivo | Dominio | Registros |
|---|---|---|
| `Volcanes.geojson` | Volcanes | 61 |
| `Fallas.geojson` | Fallas geológicas | 4.866 |
| `Mapa_Geologico_de_Colombia_2015.geojson` | Unidades geológicas | 7.461 |
| `Mapa_Tectonico_de_Colombia_2017.geojson` | Dominios tectónicos | 3 |
| `Inventario_de_movimientos_en_masa.geojson` | Movimientos en masa | 6.826 |

La aplicación lee estos archivos locales y no depende de la red en tiempo de
ejecución (modo sin conexión: el fondo vectorial de Colombia es local).

## Arquitectura

Backend Spring Boot (Java 21) con separación de responsabilidades; frontend
Leaflet en estáticos:

- **`domain/`** — modelo puro e independiente de la infraestructura:
  geometrías, dominios, entidades, repositorios.
- **`application/`** — casos de uso y servicios: autenticación, exploración de
  capas, contexto de coordenada, análisis y comparación de zonas, gestión de
  entidades, simplificación de geometría (solo visualización).
- **`infrastructure/`** — carga de datasets GeoJSON (memoria), descarga SGC,
  persistencia JSON local (`data/`), siembra del admin.
- **`web/`** — controladores REST (`/api/**`), DTOs y seguridad por sesión HTTP.
- **`static/`** — frontend Leaflet (login, mapa con capas, análisis, admin).

El dominio y el análisis siempre usan geometría completa; la simplificación de
polígonos se aplica únicamente al GeoJSON de visualización (SC-001).

## Rutas de datos

| Ruta | Contenido | Git |
|---|---|---|
| `docs/datasets/` | datasets SGC (lectura) | ignorado |
| `data/` | `users.json`, `geoentities.json` (escritura) | ignorado |
| `config/admin-account.json` | cuenta admin sembrada | versionado |
| `src/main/resources/basemap/colombia.geojson` | fondo vectorial local | versionado |

## Funcionalidades

- **Autenticación** — registro (rol USER), login/logout por sesión, admin
  sembrado (US1).
- **Exploración por capas** — 5 capas, filtros solo por atributos reales,
  procedencia SGC/GEOINSIGHT (US2).
- **Contexto de coordenada** — unidades/dominios contenientes y más cercanos
  con distancia (US3).
- **Análisis de zona** — conteos, distribuciones y listados por dominio (US4).
- **Comparación** — dos zonas con el mismo esquema de indicadores (US5).
- **Gestión admin** — crear/editar/eliminar entidades GEOINSIGHT; los registros
  SGC son de solo lectura (US6).

## Validación

La guía de escenarios de extremo a extremo (E1–E9) y los criterios de
aceptación están en `specs/001-geoinsight-core/quickstart.md`; el contrato REST
en `specs/001-geoinsight-core/contracts/api.md`.
