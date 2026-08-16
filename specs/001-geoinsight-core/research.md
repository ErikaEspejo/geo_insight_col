# Research: Sistema Núcleo de GeoInsight Colombia

**Feature**: `001-geoinsight-core` | **Date**: 2026-08-15 | **Plan**: [plan.md](./plan.md)

Fase 0 del workflow `/speckit.plan`. Consolidación de decisiones verificadas; no queda ningún `NEEDS CLARIFICATION`.

---

## 1. Obtención y mantenimiento de datasets SGC

**Decision**: Los cinco datasets se obtienen de las APIs REST oficiales del SGC (ArcGIS Feature Server) con paginación (`resultOffset`/`resultRecordCount`) y `outSR=4326` (lon/lat). Un script PowerShell (`scripts/download-datasets.ps1`) ya lo implementa y fue verificado end-to-end; el backend reutiliza la misma lógica en `infrastructure/download/SgcDatasetDownloader` para la auto-descarga al arrancar.

**Rationale**: `returnCountOnly` confirmó los conteos exactos (61/4866/7461/3/6826), que se usan para verificar la integridad de cada descarga inicial. En arranques posteriores se reutiliza el archivo local si puede cargarse y contiene entidades, sin volver a comparar el conteo oficial. `maxRecordCount` es 1000 para `MAPAGEOLOGIA` y 2000 para el resto, por eso es obligatoria la paginación.

**Alternatives considered**:

> **Corrección de trazabilidad (2026-08-16)**: los conteos oficiales versionados
> (61/4866/7461/3/6826) también se comparan localmente en cada arranque. Un
> archivo ausente, corrupto, vacío o con un conteo distinto se considera no
> disponible y se vuelve a descargar. Esta regla sustituye la frase anterior
> que permitía reutilizar cualquier archivo no vacío y mantiene la validación
> sin consultar nuevamente la API oficial.
- Descarga manual única por el usuario: descartada (decisión del usuario: auto-descarga al iniciar).
- Links de exportación `/export?f=geojson`: descartados por no paginar y devolver respuestas truncadas.
- Bundling de los GeoJSON en el repo: descartado (gitignore + decisión de auto-descarga).

### Endpoints verificados

| Dataset | Service | Capa | Conteo |
|---|---|---|---|
| MGC 2015 | `.../MAPAGEOLOGIA/FeatureServer` | 0 Volcanes | 61 |
| MGC 2015 | `.../MAPAGEOLOGIA/FeatureServer` | 1 Fallas | 4866 |
| MGC 2015 | `.../MAPAGEOLOGIA/FeatureServer` | 4 Unidades | 7461 |
| Dominios tectónicos 2017 | `.../Mapa_Tectónico_de_Colombia_2017_Dominios_Tectónicos.../FeatureServer` | 0 | 3 |
| Movimientos en masa | `.../Inventario_de_movimientos_en_masa/FeatureServer` | 0 | 6826 |

`maxRecordCount`: `MAPAGEOLOGIA` = 1000; otros = 2000.

## 2. Perfiles de datos locales

**Decision**: Los diccionarios de datos (campos, tipos, dominios de valores, nulabilidad) se derivan de los archivos locales descargados, no de documentación externa.

**Rationale**: Constitución P-03 (datos reales) y spec FR-008/FR-013/FR-018.

### Atributos reales por dominio

| Dominio | Archivo | Atributos presentes |
|---|---|---|
| Fallas | `Fallas.geojson` | `OBJECTID`, `Tipo`, `NombreFalla`, `Comentarios`, `GlobalID`, `Shape__Length` |
| Movimientos | `Inventario_de_movimientos_en_masa.geojson` | `FID`, `OBJECTID`, `ID`, `INV_MOVIMI`, `F35DOV_TIP`, `TIPO`, `SUBTIPO_MO`, `SUBTIPO`, `CLAS_MAPA`, `ETIQUETA_M`, `ESRI_OID` |
| Unidades | `Mapa_Geologico_de_Colombia_2015.geojson` | `OBJECTID`, `SimboloUC`, `N°CartaColores`, `Descripcion`, `Edad`, `UGIntegradas`, `Comentarios`, `GlobalID`, `Shape__Area`, `Shape__Length` |
| Dominios | `Mapa_Tectonico_de_Colombia_2017.geojson` | `FID`, `OBJECTID`, `CodigoDT`, `NombreDT`, `AreaDT`, `SHAPE_Leng`, `SHAPE_Area`, `Label`, `Shape__Area`, `Shape__Length` |
| Volcanes | `Volcanes.geojson` | `OBJECTID`, `VolcanID`, `NombreVolcan`, `AlturaSobreNivelMar`, `Latitud`, `Longitud`, `Comentarios`, `URL`, `GlobalID` |

### Geometrías

| Dominio | Geometría principal |
|---|---|
| Movimiento en masa | Point |
| Volcán | Point |
| Falla geológica | LineString |
| Unidad geológica | Polygon |
| Dominio tectónico | Polygon |

**Decisión**: aceptar variantes multiparte reales del GeoJSON (`MultiPoint`, `MultiLineString`, `MultiPolygon`) mediante una jerarquía de geometría en dominio. Si el dataset real presenta multiparte, se modela como tal; no se simplifica silenciosamente.

### Campos administrativos y tipos observados

**Decisión**: la captura GEOINSIGHT usa una lista blanca de campos descriptivos reales. Se excluyen metadatos del proveedor y medidas derivadas (`OBJECTID`, `FID`, `GlobalID`, `ESRI_OID`, `Shape__Area`, `Shape__Length`, `SHAPE_*`). Un campo completamente nulo tampoco se ofrece porque el dataset no aporta evidencia para inferir su tipo.

Tipos confirmados en los datos locales:

| Dominio | Campos numéricos observados | Campos descriptivos editables |
|---|---|---|
| Movimientos | `ID`, `INV_MOVIMI` (enteros) | `TIPO`, `SUBTIPO`, `CLAS_MAPA`, `ETIQUETA_M` |
| Fallas | — | `Tipo`, `NombreFalla` |
| Unidades | — | `SimboloUC`, `Descripcion`, `Edad`, `UGIntegradas`, `Comentarios` |
| Dominios | — | `CodigoDT`, `NombreDT`, `Label` |
| Volcanes | `AlturaSobreNivelMar` (entero) | `NombreVolcan`, `Comentarios`, `URL` |

`VolcanID`, `N°CartaColores` y `Comentarios` de fallas están presentes pero sin valores observables en los archivos actuales; por tanto no se tipan ni se capturan. Los vocabularios de `TIPO`, `SUBTIPO`, `CLAS_MAPA`, `Tipo` y `Edad` se reutilizan como opciones predefinidas.

**Alternativa descartada**: aceptar un objeto JSON libre. Permitía claves inventadas y convertía números en texto desde la UI. La lista blanca y `AttributeValueType` se validan en aplicación, no solo en presentación.

## 3. Tamaños y estrategia de render

**Decision**: Carga total en memoria al arranque y búsquedas espaciales lineales por dominio; el backend sirve GeoJSON de visualización por dominio y el frontend combina Leaflet con Canvas para las capas puntuales densas.

**Rationale**: Tamaños medidos — unidades 119.2 MB, fallas 8.1 MB, dominios 4.4 MB, movimientos 2.2 MB, volcanes <1 MB (~119 MB total). En una app local de un solo usuario es viable en memoria (SC-001..SC-003).

**Contingencia SC-001**: si la capa de unidades (7461 polígonos) degrada el render del navegador, se sirve geometría **simplificada solo para visualización** (Douglas-Peucker), mientras análisis y consultas usan siempre la geometría completa. Se decide en la tarea de frontend.

**Alternatives considered**:
- PostGIS/PGis en PostgreSQL: descartado (constitución prohíbe BD).
- Turf.js en el cliente: descartado (el mapa no reemplaza la lógica; context §5).
- Servir los archivos raw como estáticos al navegador: descartado (perdería el control de procedencia y filtros del backend).

## 4. Fondo de mapa sin conexión

**Decision**: Fondo vectorial local mínimo (contorno de Colombia, e idealmente departamentos) servido como capa base por el backend; los tiles OSM se cargan solo si hay internet. Sin conexión, el mapa muestra el fondo vectorial local y todas las capas (los datos siempre vienen del backend local).

**Rationale**: La decisión del usuario es "los datos deben estar siempre disponibles"; el fondo OSM es decorativo y no debe ser dependencia funcional.

**Alternatives considered**:
- Bundle de tiles offline: descartado (volumen excesivo para un proyecto de curso).
- Fondo gris vacío offline: descartado por UX pobre; el contorno de Colombia da contexto geográfico mínimo.

## 5. Autenticación y autorización

**Decision**:
- Todo acceso exige sesión (spec FR-021, SC-008). Spring Security con sesión HTTP y login JSON.
- Contraseñas con hash BCrypt (nunca texto plano) — FR-024.
- Cuenta admin **sembrada** en `config/admin-account.json` (usuario + hash BCrypt) y poblada en `data/users.json` al primer arranque si no existe — FR-023.
- Registro abierto para usuarios de consulta; **ninguna cuenta registrada obtiene rol ADMIN** (invariante en dominio, no solo en UI) — FR-022.
- Rutas `/api/admin/**` exigen rol ADMIN — FR-025.
- La pantalla de acceso ofrece solo login y registro local; sus imágenes y logotipos se empaquetan como recursos públicos. No se anuncia SSO porque no existe un proveedor institucional integrado.

**Rationale**: Decisión del usuario ("login obligatorio para todos", "el admin es una cuenta en un JSON ya estable", "los que consultan pueden crear cuentas y registrarse, pero ninguna será admin").

**Alternatives considered**:
- Sin autenticación (rol por configuración): descartada por decisión explícita del usuario.
- JWT: descartado (sesión HTTP es más simple y suficiente para un solo usuario local).
- Ruta de registro con flag admin: descartado por inseguro y contra la decisión del usuario.

## 6. Semántica de análisis geoespacial

**Decision** (reglas explícitas y comprobables, trazables a la spec):
- **Distancia**: haversine entre puntos; distancia punto-a-segmento para fallas; la distancia reportada es la mínima entre la coordenada consultada y la geometría (soporta multiparte).
- **Contención** (unidades y dominios tectónicos): ray casting punto-en-polígono; multiparte evalúa cada parte.
- **Empates de proximidad** (edge case): se desempata por el identificador completo en orden lexicográfico ascendente, regla uniforme para IDs SGC y GEOINSIGHT.
- **Cobertura de consulta puntual**: se usa una cascada por disponibilidad. Primero dominios tectónicos; si ese dataset no está disponible, unidades geológicas; si ambos están ausentes, el borde local del basemap de Colombia. No se avanza en la cascada porque el punto no esté contenido.
- **Coordenada fuera de cobertura**: respuesta `insideCoverage=false`, contenedores vacíos y vecinos `null`; el análisis y la comparación continúan usando exclusivamente la intersección con sus radios y no aplican este recorte.
- **Zona**: radio en metros; movimiento/volcán dentro si distancia centro-punto ≤ radio; falla presente si distancia centro-línea ≤ radio; unidad/dominio presente si el polígono contiene el centro o intersecta la circunferencia de la zona.
- **Comparación**: mismo conjunto de indicadores en ambas zonas, solo diferencias descriptivas (FR-011, SC-004).
- **Comparación**: cada lado reutiliza el análisis del radio. Los vecinos más cercanos se eligen únicamente entre las entidades ya incluidas dentro o intersectadas por ese radio; no se agrega contexto puntual central.
- **Estados**: `count: 0` significa dataset disponible sin coincidencias; `dataAvailable: false` significa dataset ausente/no cargado; atributo vacío y error conservan mensajes distintos.
- **Sin riesgo/amenaza** (FR-012, SC-005): los indicadores son conteos, distribuciones, nombres y distancias; sin adjetivos de peligrosidad.

## 7. Decisiones de interacción cartográfica

- Las capas temáticas inician desactivadas. Cada toggle invalida solicitudes pendientes para impedir que una respuesta tardía reactive una capa apagada.
- El control de capas flota en la esquina inferior derecha del mapa, sobre la barra de coordenadas, y combina el símbolo cartográfico convencional de tres capas apiladas (SVG) con la etiqueta «Capas». El desplegable abre hacia arriba, conserva tooltip, etiqueta accesible, foco visible y estado expandido, y se cierra con clic fuera o Escape.
- La herramienta lateral «Explorar mapa» se eliminó: la lista de capas vive únicamente en el control flotante. El panel contextual inicia colapsado con «Buscar y filtrar» como módulo predeterminado, evitando un panel vacío al expandir; el zoom nativo de Leaflet permanece sin superposición con el control de capas.
- Los puntos densos (6826 movimientos) se renderizan en canvas con hit-testing propio; líneas y polígonos permanecen en Leaflet GeoJSON.
- La consulta por coordenada responde a clics únicamente cuando su módulo está activo. Zona A, Zona B y análisis de zona usan un selector explícito de un solo clic.
- Los campos de coordenadas permanecen vacíos y muestran el formato esperado mediante placeholders; el selector del mapa reemplaza esos valores cuando se activa explícitamente.
- Comparación dibuja centros A/B y círculos diferenciados y ajusta el viewport a la unión de ambos bounds.
- La consulta por coordenada organiza sus resultados en tres secciones (Resultado, Contexto geológico, Elementos cercanos) con tarjetas compactas. Los nombres descriptivos del dataset tienen prioridad sobre los identificadores técnicos (mostrados de forma secundaria); las distancias usan metros bajo 1 km y kilómetros desde 1 km, con un decimal y separador decimal español.
- En el mapa, la consulta por coordenada marca la ubicación con un símbolo propio (diana azul con pulso), resalta las geometrías contenedoras con borde y relleno transparente, y dibuja la falla, el movimiento en masa y el volcán más cercanos con estilos propios por dominio. Al consultar desde el formulario ajusta la vista a la unión de los elementos resaltados (con un zoom máximo que conserva el contexto); al consultar con clic conserva la vista natural.
- Las entidades de las capas temáticas y de los resultados de zona/comparación muestran una vista previa al pasar el cursor (nombre, procedencia y atributos principales) mediante tooltip de Leaflet; los puntos densos en canvas reutilizan el hit-testing del clic para abrir el mismo tooltip, y el clic sigue fijando el detalle completo en el panel.
- Consulta por coordenada, análisis de zona y comparación incluyen una acción «Borrar» que restablece el panel a su estado vacío y limpia en el mapa solo los overlays propios del análisis (marcador/resaltados, centro/radio/entidades, centros A/B), conservando las capas temáticas activas.
- Administración captura geometrías con clics: uno para Point, mínimo dos para LineString y mínimo tres para Polygon; el anillo se cierra al finalizar.

**Alternatives considered**:
- Fórmulas simplificadas (Euclidiana): descartadas (harían errónea la distancia en latitud/coordenadas reales).
- Biblioteca JTS: descartada (la geometría con OOP propio demuestra el dominio del curso y evita dependencia externa; es la única abstracción de geometría necesaria).

## 8. Persistencia de entidades GEOINSIGHT y cuentas

**Decision**: Archivos JSON locales `data/geoentities.json` y `data/users.json`, escritos con escritura atómica (temp + rename), detrás de interfaces de repositorio del dominio. El dominio nunca serializa; la serialización es responsabilidad de infraestructura.

**Rationale**: Construcción acordada (JSON local detrás de repositorios, sin BD).

## 9. Framework y stack

**Decision**: Java 21 + Maven 3.9.16 (con wrapper) + Spring Boot 3.3.x web/security/validation + Jackson + Leaflet 1.9 vendoreado localmente + JUnit 5/AssertJ.

**Rationale**: Verificado en la máquina (Java 21.0.11, Maven 3.9.16). Spring Boot sirve API REST y estáticos; es el marco acordado en AGENTS.md (dominio independiente de Spring, la infraestructura sí lo usa).

**Alternatives considered**:
- Spring Boot 4.x/Spring 7: no; se elige 3.3.x estable probado con Java 21.
- Servidor embedido Jetty: no; Tomcat por defecto.
- React/Vue: descartados (YAGNI; Leaflet + JS vanilla suficiente).
