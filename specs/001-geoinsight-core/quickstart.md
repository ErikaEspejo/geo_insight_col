# Quickstart: Sistema Núcleo de GeoInsight Colombia

**Feature**: `001-geoinsight-core` | **Date**: 2026-08-15 | **Plan**: [plan.md](./plan.md)

Guía de validación de extremo a extremo. Detalles de implementación en `tasks.md`; contratos en `contracts/api.md`; modelo en `data-model.md`.

## Prerrequisitos

- Java 21 (verificado: `21.0.11`)
- Maven 3.9.x o el Maven Wrapper (`./mvnw`)
- Internet solo en el **primer** arranque (bootstrap descarga datasets y pide tiles OSM; los datos luego son locales)

## Configuración inicial

1. Cuenta admin sembrada en `config/admin-account.json` (usuario + hash BCrypt). Contenido esperado:

   ```json
   { "username": "admin", "passwordHash": "<hash-bcrypt>" }
   ```

2. `data/` (gitignored) se crea solo al primer arranque: `users.json`, `geoentities.json`.

## Compilar y ejecutar

```bash
./mvnw clean verify   # compila + tests (Linux/macOS)
./mvnw.cmd clean verify   # Windows PowerShell
./mvnw spring-boot:run
```

Al arrancar el backend:
- verifica los 5 datasets en `docs/datasets/`; si falta alguno, los descarga (primera vez);
- siembra el admin en `data/users.json` si no existe.

## Escenarios de validación

### E1. Autenticación (SC-008, FR-021..FR-025, FR-036)
1. Abrir `http://localhost:8080` sin sesión → redirige a `login.html`.
2. Registrar `ana` / contraseña → `201` con rol `USER`.
3. Logout y login con credenciales incorrectas → `401` con mensaje claro.
4. Login con credenciales correctas → accede; `GET /api/auth/me` devuelve rol.
5. Login admin (cuenta sembrada) → `GET /api/auth/me` devuelve rol `ADMIN`.
6. `ana` (USER) llama a `POST /api/admin/entities` → `403`.
7. Abrir el login sin sesión → logotipo e imagen cargan correctamente y solo aparecen inicio de sesión y registro; no aparece SSO institucional.

### E2. Exploración por capas (SC-001, SC-007, FR-001..FR-008, FR-026..FR-028, FR-039..FR-042)
1. Con sesión, abrir el mapa → cinco capas listadas y ninguna activa; activar capas → entidades visibles (puntos, líneas, polígonos).
2. Desactivar una capa → sus entidades dejan de verse.
3. Seleccionar una entidad → panel muestra atributos reales y procedencia (SGC/GEOINSIGHT).
4. Filtrar por un atributo real (ej. `TIPO` en movimientos) → solo entidades que cumplen.
5. No existe filtro para atributos inexistentes en el UI.
6. Agregar dos valores del mismo atributo → unión OR; agregar otro atributo → intersección AND.
7. La tabla de coincidencias se actualiza con cada filtro; clic en una fila → zoom y detalle. Cambiar la capa del filtro limpia los criterios anteriores.
8. Seleccionar un volcán o movimiento en masa → el detalle presenta etiquetas y valores legibles sin fragmentación carácter por carácter.
9. El control de capas aparece flotante en la esquina inferior derecha del mapa, con el icono convencional de capas apiladas y la etiqueta “Capas”; al abrirlo, el selector se muestra hacia arriba sin quedar oculto por el panel de entidad.
10. Cerrar el selector con clic fuera o Escape → se cierra y el botón restablece su estado; la herramienta lateral “Explorar mapa” ya no existe en el menú.
11. Recargar la aplicación → el panel contextual inicia colapsado y sin módulo activo; al elegir una herramienta se abre su módulo y el chevrón cambia de forma consistente al abrir/cerrar.

### E3. Consulta por coordenada (SC-002, FR-009, FR-014, FR-016, FR-029, FR-043, FR-044, FR-045, FR-046)
1. `POST /api/context` con coordenada dentro de cobertura → unidad conteniente, dominio conteniente, falla/movimiento/volcán más cercanos con distancias.
2. Coordenada fuera de toda cobertura → ausencia explícita: contenedores vacíos (`geologicalUnits`/`tectonicDomains` = `[]`) y más cercanos con su distancia real (nunca datos inventados; `nearest*` solo es `null` si el dominio no tiene entidades), cumpliendo FR-014.
3. Coordenada inválida (lat 95) → `400` con mensaje claro.
4. Clic en el mapa dentro de Consulta por coordenada → completa y consulta; el mismo clic desde otra pestaña no abre este módulo.
5. Consulta desde el formulario → resultados en tarjetas por sección (Resultado, Contexto geológico, Elementos cercanos), distancias en m (< 1 km) o km (≥ 1 km) con un decimal, nombres descriptivos priorizados; dominio sin datos → mensaje legible, sin `null` ni JSON.
6. La vista del mapa ajusta para mostrar el marcador propio de la ubicación, los contenedores resaltados y la falla, el movimiento y el volcán más cercanos (zoom máximo conservado); el clic en el mapa conserva la vista natural.

### E4. Análisis de zona (SC-003, FR-010, FR-012, FR-016, FR-029, FR-030, FR-038)
1. `POST /api/zones/analyze` con centro y radio → conteos, distribuciones por `TIPO`/`SUBTIPO`/`CLAS_MAPA`, listados por dominio.
2. Zona sin registros de un dominio → `count: 0` / lista vacía, sin frases de riesgo.
3. Radio ≤ 0 → `400`.
4. Elegir centro desde el mapa → completa campos; ejecutar → marcador central, círculo y viewport ajustado.
5. Antes de ingresar datos, longitud y latitud están vacías y sus placeholders indican el formato esperado.

### E5. Comparación (SC-004, FR-011, FR-029..FR-031, FR-038)
1. `POST /api/zones/compare` con dos zonas y radio común → ambas columnas con el mismo esquema de indicadores.
2. Diferencias solo descriptivas (cantidades/distancias), nunca de riesgo.
3. Elegir A/B desde el mapa y comparar → centros y radios diferenciados, ambos completos en viewport.
4. Verificar tarjetas por movimientos, fallas, geología, tectónica y volcanismo con conteos, predominancia cuando aplica, contexto central y vecinos con distancia.
5. Verificar que `0`, `Información no disponible`, `Atributo sin valor` y error se presentan como estados distintos; el vecino más cercano se diferencia de entidades dentro del radio.
6. Los campos A/B inician vacíos con placeholders y cada selector del mapa actualiza únicamente su zona.

### E6. Gestión admin (SC-005, SC-006, SC-009, FR-003..FR-005, FR-018, FR-019, FR-032..FR-034)
1. Admin crea un volcán GEOINSIGHT con los campos obligatorios del dominio → `201` con `origin: GEOINSIGHT`.
2. La entidad GEOINSIGHT aparece en el mapa, consulta y análisis, sin presentarse como SGC.
3. Admin edita/elimina esa entidad → persiste; aparece en `data/geoentities.json`.
4. Admin intenta editar/eliminar una entidad SGC → `403`; los registros SGC quedan intactos (contador por dominio igual al del dataset).
5. El formulario muestra solo campos descriptivos permitidos, selectores para vocabularios reales y controles numéricos para `ID`, `INV_MOVIMI` y `AlturaSobreNivelMar`.
6. Enviar `CampoInventado` o `"ID":"123"` → `400`; enviar `"ID":123` con los demás campos válidos → aceptado.
7. Dibujar punto/línea/polígono según dominio; geometría incompatible → `400` aun si se construye manualmente la petición.
8. “Mis entidades” permanece visible, usa colores por dominio y actualiza mapa/lista tras crear, editar o eliminar.
9. Eliminar → modal GeoInsight; cancelar no llama a DELETE y confirmar sí elimina.

### E7. Datos ausentes (FR-020)
1. Mover `docs/datasets/Volcanes.geojson` fuera de su lugar → al arrancar se redescarga; si la red no está disponible, el sistema arranca con indicador inequívoco de datos ausentes para ese dominio.

### E8. Sin conexión
1. Con datos ya descargados, desconectar internet y arrancar → el mapa muestra las capas sobre el fondo vectorial local (Colombia); análisis y consultas funcionan.

### E9. Ayuda, navegación y visibilidad por rol (FR-035, FR-037)
1. Usuario abre Ayuda → instrucciones de exploración, filtros, contexto, zona y comparación; no aparece Administración.
2. Admin abre Ayuda → también aparece la explicación administrativa.
3. El encabezado no muestra búsqueda global, ayuda duplicada ni usuario duplicado; la ayuda vive en el menú y la identidad de sesión en el bloque inferior izquierdo.

## Criterios de aceptación (SC)

| Criterio | Verificación |
|---|---|
| SC-001 mapa < 10 s | E2 (cronometrar activación de capas, especial unidades) |
| SC-002 contexto < 5 s | E3 |
| SC-003 zona < 5 s | E4 |
| SC-004 misma vista lado a lado | E5 |
| SC-005 sin riesgo/amenaza | E4, E5 (inspeccionar respuestas) |
| SC-006 SGC intactos | E6.4 |
| SC-007 filtros reales | E2.4/2.5 |
| SC-008 auth obligatoria | E1.1 |
| SC-009 captura administrativa tipada | E6.5/6.6/6.7 |
| SC-010 herramientas cartográficas aisladas | E3.4, E4.4, E5.3, E6.7 |
