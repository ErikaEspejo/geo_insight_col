# Feature Specification: Sistema Núcleo de GeoInsight Colombia

**Feature Branch**: `001-geoinsight-core`

**Created**: 2026-08-15

**Status**: Implemented and synchronized with the current application

**Input**: User description: "Implementar el sistema completo de GeoInsight Colombia" + decisiones de usuario: login obligatorio para todos, cuenta admin sembrada, usuarios de consulta registrables, aplicación web (backend + frontend).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Autenticación y registro de usuarios (Priority: P1)

Todo acceso al sistema exige autenticación. El administrador inicia sesión con la cuenta sembrada por configuración (archivo JSON local). Los usuarios de consulta pueden registrarse creando una cuenta propia y, al iniciar sesión, acceden con rol de usuario. Ninguna cuenta registrada obtiene rol administrador.

**Why this priority**: Es la puerta de entrada de todo el sistema; sin autenticación ningún flujo de exploración o análisis es alcanzable.

**Independent Test**: Un usuario sin sesión no accede al mapa; un usuario registrado inicia sesión y consulta; el administrador inicia sesión y gestiona entidades; un usuario registrado nunca puede gestionar entidades.

**Acceptance Scenarios**:

1. **Given** un visitante sin sesión, **When** intenta acceder al sistema, **Then** es redirigido a la pantalla de inicio de sesión.
2. **Given** un usuario registrado con credenciales correctas, **When** inicia sesión, **Then** accede al sistema con rol de usuario.
3. **Given** credenciales incorrectas, **When** se intenta iniciar sesión, **Then** el sistema rechaza el acceso con un mensaje claro.
4. **Given** un visitante, **When** se registra una cuenta de consulta, **Then** la cuenta queda con rol usuario (nunca administrador).
5. **Given** un nombre de usuario ya existente, **When** se intenta registrar, **Then** el sistema rechaza el registro con un mensaje claro.
6. **Given** la cuenta administrador sembrada, **When** el administrador inicia sesión, **Then** accede al sistema con rol administrador.
7. **Given** un usuario con rol usuario autenticado, **When** intenta crear, editar o eliminar entidades, **Then** el sistema lo impide.
8. **Given** un visitante sin sesión, **When** abre la pantalla de acceso, **Then** ve la identidad visual de GeoInsight y únicamente las opciones de inicio de sesión y registro local, sin SSO institucional.

---

### User Story 2 - Exploración cartográfica por capas (Priority: P1)

El usuario explora los cinco dominios geocientíficos como capas diferenciadas sobre un mapa, activa y desactiva cada capa, selecciona entidades y consulta sus atributos. Puede buscar y filtrar entidades mediante atributos que existan realmente en cada dataset.

**Why this priority**: Es la puerta de entrada de toda la exploración; sin el mapa por capas ningún análisis posterior es alcanzable.

**Independent Test**: Se carga el mapa con las cinco capas, se alterna su visibilidad y se consultan los atributos de una entidad seleccionada; entrega valor de consulta inmediato.

**Acceptance Scenarios**:

1. **Given** el sistema con los cinco datasets cargados, **When** el usuario abre el mapa, **Then** ve los cinco dominios como capas diferenciadas.
2. **Given** el mapa visible, **When** el usuario desactiva una capa, **Then** las entidades de esa capa dejan de mostrarse.
3. **Given** una entidad seleccionada, **When** el usuario consulta sus atributos, **Then** se muestran los atributos reales del dataset y su procedencia (SGC o GEOINSIGHT).
4. **Given** el usuario busca por un atributo que existe en el dataset, **When** ejecuta la búsqueda, **Then** se muestran solo las entidades que cumplen el criterio.
5. **Given** el usuario intenta filtrar por un atributo inexistente, **When** ejecuta el filtro, **Then** el sistema no ofrece ese criterio (no se inventan atributos).
6. **Given** dos valores del mismo atributo, **When** se aplican como filtros, **Then** se combinan con OR; filtros de atributos distintos se combinan con AND.
7. **Given** un filtro aplicado, **When** se muestran coincidencias, **Then** una tabla lista los registros y al seleccionar una fila el mapa hace zoom a la entidad.
8. **Given** el usuario cambia la capa del constructor de filtros, **When** se carga la nueva capa, **Then** se eliminan los filtros de la capa anterior.
9. **Given** el usuario abre el mapa, **When** aún no activa capas, **Then** ninguna capa temática se encuentra activa por defecto.
10. **Given** una entidad puntual seleccionada, **When** se abre su detalle, **Then** etiquetas y valores permanecen legibles sin fragmentarse carácter por carácter ni exigir desplazamiento horizontal innecesario.
11. **Given** el mapa visible, **When** el usuario ubica el control de capas en la esquina inferior derecha, **Then** encuentra el icono convencional de capas apiladas con la etiqueta “Capas” y, al pulsarlo, el selector se abre hacia arriba sin quedar oculto por el panel de entidad.
12. **Given** el selector de capas abierto, **When** el usuario hace clic fuera del control o presiona Escape, **Then** el selector se cierra y el estado expandido del botón se restablece.
13. **Given** la aplicación recién abierta, **When** el usuario aún no elige otra herramienta, **Then** el panel contextual permanece colapsado con «Buscar y filtrar» como módulo predeterminado; al expandirlo nunca se muestra un panel vacío.
14. **Given** el mapa con entidades visibles, **When** el usuario pasa el cursor sobre una entidad, **Then** aparece una vista previa con su nombre, procedencia y atributos principales; al hacer clic se fija el detalle completo en el panel.

---

### User Story 3 - Consulta del contexto de una coordenada (Priority: P1)

El usuario ingresa o selecciona una coordenada y el sistema responde qué contexto geocientífico tiene esa ubicación: unidad geológica y dominio tectónico que la contienen, falla más cercana con su distancia, movimiento en masa más cercano con su distancia y volcán más cercano con su distancia.

**Why this priority**: Es el propósito central del sistema, declarado en el contexto funcional acordado.

**Independent Test**: Se ingresa una coordenada dentro de la cobertura de los datasets y se verifica que cada dominio devuelva resultado o una ausencia explícita.

**Acceptance Scenarios**:

1. **Given** una coordenada dentro de la cobertura, **When** el usuario la consulta, **Then** el sistema devuelve los resultados por dominio con su distancia cuando aplica.
2. **Given** una coordenada donde un dominio no tiene resultado, **When** se consulta, **Then** el sistema lo indica explícitamente sin inventar datos.
3. **Given** una coordenada no válida, **When** se consulta, **Then** el sistema la rechaza con un mensaje claro.
4. **Given** empates de proximidad o geometrías superpuestas, **When** se consulta, **Then** el sistema aplica la regla definida en la especificación de datos.
5. **Given** otra herramienta activa, **When** el usuario hace clic en el mapa, **Then** no se ejecuta ni se abre la consulta por coordenada; el clic consulta coordenadas solo dentro de su módulo.
6. **Given** una consulta con resultados, **When** se presentan, **Then** se organizan en tres secciones (Resultado, Contexto geológico, Elementos cercanos) y cada dominio se muestra en una tarjeta compacta con nombre descriptivo y distancia legible.
7. **Given** una consulta ejecutada desde el formulario, **When** llegan los resultados, **Then** el mapa marca la ubicación consultada con un símbolo propio, resalta los contenedores y los elementos más cercanos (incluido el volcán) y ajusta la vista para mostrarlos todos, sin superar un zoom máximo.
8. **Given** un clic en el mapa dentro del módulo, **When** se ejecuta la consulta, **Then** el mapa conserva la vista natural sin cambios bruscos de zoom.
9. **Given** un dominio sin información, **When** se presentan los resultados, **Then** se muestra un mensaje legible sin valores `null`, estructuras JSON ni identificadores vacíos.
10. **Given** una consulta ya presentada, **When** el usuario pulsa «Borrar consulta», **Then** el panel vuelve a su estado vacío y el mapa deja de mostrar el marcador y los elementos resaltados de esa consulta.
11. **Given** una consulta puntual, **When** se determina su cobertura, **Then** se usa primero el dataset disponible de dominios tectónicos, en su ausencia el de unidades geológicas y, si ambos están ausentes, el borde local del basemap de Colombia.
12. **Given** una coordenada fuera de la cobertura seleccionada por esa cascada, **When** se consulta, **Then** la respuesta exitosa indica `insideCoverage=false`, no calcula vecinos y la interfaz informa la ausencia de cobertura.

---

### User Story 4 - Análisis de una zona (Priority: P2)

El usuario define una coordenada central y un radio; el sistema produce una caracterización descriptiva del entorno: cantidad de movimientos en masa dentro de la zona, distribución por tipo y subtipo, fallas presentes o próximas, unidades geológicas y dominios tectónicos presentes, y volcanes cercanos.

**Why this priority**: Amplía la consulta puntual a un análisis de entorno; depende de la consulta por coordenada para tener sentido pleno.

**Independent Test**: Se define un centro y un radio sobre zonas conocidas y se verifican los conteos, distribuciones y listados por dominio.

**Acceptance Scenarios**:

1. **Given** una zona con registros dentro del radio, **When** se analiza, **Then** se presentan los conteos, distribuciones y entidades relevantes por dominio.
2. **Given** una zona sin registros de algún dominio, **When** se analiza, **Then** se indica la ausencia de forma explícita para ese dominio.
3. **Given** un radio no válido, **When** se analiza, **Then** el sistema rechaza el radio con un mensaje claro.
4. **Given** resultados presentados, **When** se muestran, **Then** no incluyen conclusiones de riesgo, amenaza, peligrosidad ni seguridad.
5. **Given** una zona válida, **When** se analiza, **Then** el mapa representa el centro y el radio y ajusta el viewport a la zona completa.
6. **Given** el formulario de zona, **When** el usuario activa “Elegir punto en el mapa” y hace clic, **Then** longitud y latitud se completan sin ejecutar otro módulo.
7. **Given** un análisis ya presentado, **When** el usuario pulsa «Borrar análisis», **Then** el panel vuelve a su estado vacío y el mapa elimina el centro, el radio y las entidades resaltadas de la zona.

---

### User Story 5 - Comparación descriptiva de dos zonas (Priority: P2)

El usuario selecciona dos coordenadas y un radio común de análisis; el sistema analiza ambas zonas con los mismos criterios y presenta indicadores equivalentes lado a lado.

**Why this priority**: Es un análisis derivado del análisis de zona; útil para la caracterización comparativa acordada.

**Independent Test**: Se seleccionan dos zonas y se verifica que ambas columnas muestren los mismos indicadores calculados con los mismos criterios.

**Acceptance Scenarios**:

1. **Given** dos coordenadas y un radio común, **When** se compara, **Then** se muestran los mismos indicadores para ambas zonas, lado a lado.
2. **Given** una zona con más registros que la otra, **When** se comparan, **Then** el sistema solo afirma diferencias descriptivas (cantidades, distancias) y nunca diferencias de riesgo o seguridad.
3. **Given** una comparación válida, **When** se presenta, **Then** los centros A/B y sus radios se diferencian visualmente y el viewport incluye ambas zonas completas.
4. **Given** resultados disponibles, **When** se presenta cada dominio, **Then** se muestran lado a lado conteos, distribuciones, entidades dentro o intersectadas por el radio y el vecino más cercano entre esas mismas entidades.
5. **Given** un dominio sin registros, un dataset ausente o un atributo vacío, **When** se presenta el resultado, **Then** esos estados se distinguen explícitamente.
6. **Given** los formularios A/B, **When** el usuario elige cada punto en el mapa, **Then** solo se completan los campos del destino seleccionado.
7. **Given** una comparación ya presentada, **When** el usuario pulsa «Borrar comparación», **Then** el panel vuelve a su estado vacío y el mapa elimina los centros A/B y sus radios.

---

### User Story 6 - Incorporación y gestión de entidades propias (Priority: P3)

El administrador crea, edita y elimina entidades de origen GEOINSIGHT en cualquiera de los cinco dominios, respetando la geometría admitida por cada dominio. Los registros SGC permanecen inmutables y el usuario normal no gestiona entidades.

**Why this priority**: Depende de los flujos de consulta y análisis para tener valor; es el flujo administrativo menos crítico.

**Independent Test**: Se crea una entidad en cada dominio, se edita y elimina una entidad GEOINSIGHT, y se verifica que ningún registro SGC cambie y que el usuario normal no pueda gestionar entidades.

**Acceptance Scenarios**:

1. **Given** un administrador, **When** crea una entidad en un dominio, **Then** la entidad queda registrada con origen GEOINSIGHT.
2. **Given** una entidad GEOINSIGHT, **When** el administrador la edita o elimina, **Then** el cambio se aplica y persiste.
3. **Given** un registro SGC, **When** cualquier actor intenta editarlo o eliminarlo, **Then** el sistema lo impide.
4. **Given** una entidad SGC consultada, **When** se muestran sus datos, **Then** su procedencia es visible como SGC.
5. **Given** una entidad GEOINSIGHT consultada, **When** se muestran sus datos, **Then** su procedencia es visible como GEOINSIGHT y no se presenta como información oficial del SGC.
6. **Given** un usuario normal, **When** intenta crear, editar o eliminar entidades, **Then** el sistema lo impide.
7. **Given** un administrador creando una entidad, **When** selecciona el dominio, **Then** el formulario ofrece únicamente campos descriptivos reales, sus tipos observados y los valores categóricos disponibles.
8. **Given** un atributo técnico, desconocido o con tipo incorrecto, **When** se intenta enviar, **Then** el caso de uso rechaza la operación aunque la petición no provenga de la UI.
9. **Given** el dominio seleccionado, **When** el administrador dibuja en el mapa, **Then** se captura Point, LineString o Polygon según la geometría admitida.
10. **Given** entidades GEOINSIGHT existentes, **When** se abre Administración, **Then** permanecen visibles en una lista y en el mapa, diferenciadas por color de dominio.
11. **Given** una solicitud de eliminación, **When** el administrador presiona eliminar, **Then** un modal propio solicita confirmación antes de ejecutar la operación.

---

### Edge Cases

- Qué sucede cuando un dataset contiene geometrías multiparte (`MultiPoint`, `MultiLineString`, `MultiPolygon`).
- Cómo se comporta el sistema ante geometrías nulas, vacías o inválidas.
- Qué ocurre cuando la coordenada consultada cae fuera de toda cobertura.
- Cómo se resuelve una coordenada dentro de polígonos superpuestos.
- Cómo se manejan atributos con valores nulos o cadenas vacías.
- Cómo se resuelven los empates de proximidad (dos entidades a igual distancia).
- Cómo se trata una entidad que intersecta parcialmente el límite de una zona.
- Qué sucede cuando no hay datasets cargados o un archivo está corrupto.
- Qué sucede cuando un usuario intenta registrar un nombre de usuario ya existente.
- Qué sucede cuando un usuario con rol usuario intenta acceder a funciones administrativas.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE cargar los cinco datasets SGC y exponer sus entidades.
- **FR-002**: Cada entidad DEBE conservar su procedencia, SGC o GEOINSIGHT.
- **FR-003**: Los registros SGC DEBEN ser de solo lectura.
- **FR-004**: Solo el administrador DEBE poder crear entidades.
- **FR-005**: Solo el administrador DEBE poder editar o eliminar entidades GEOINSIGHT.
- **FR-006**: El mapa DEBE mostrar los cinco dominios como capas diferenciadas y permitir activar o desactivar cada una.
- **FR-007**: El sistema DEBE permitir seleccionar una entidad y consultar sus atributos reales.
- **FR-008**: El sistema DEBE permitir buscar y filtrar solo mediante atributos que existan realmente en el dataset.
- **FR-009**: El sistema DEBE permitir consultar el contexto geocientífico de una coordenada.
- **FR-010**: El sistema DEBE permitir analizar una zona definida por coordenada central y radio.
- **FR-011**: El sistema DEBE permitir comparar descriptivamente dos zonas aplicando los mismos criterios.
- **FR-012**: El sistema NO DEBE calcular ni comunicar riesgo, amenaza, vulnerabilidad, peligrosidad, probabilidad de ocurrencia, seguridad ni predicciones.
- **FR-013**: La semántica de contención, intersección, cercanía y distancia DEBE definirse con evidencia de los datasets, no inventarse.
- **FR-014**: La ausencia de resultados DEBE indicarse explícitamente.
- **FR-015**: La procedencia de cada entidad DEBE ser visible en las consultas.
- **FR-016**: Las coordenadas y radios DEBEN validarse antes de procesarse.
- **FR-017**: Las geometrías DEBEN tratarse respetando su tipo, CRS y orden de coordenadas.
- **FR-018**: Los campos obligatorios y dominios de valores para crear entidades DEBEN derivarse de los datasets.
- **FR-019**: Una entidad GEOINSIGHT DEBE poder participar en consultas y análisis sin presentarse como SGC.
- **FR-020**: El sistema DEBE indicar de forma inequívoca la ausencia de un dataset o de datos cargados.
- **FR-021**: Todo acceso al sistema DEBE exigir autenticación previa.
- **FR-022**: El sistema DEBE permitir registrar cuentas de usuario de consulta; ninguna cuenta registrada DEBE obtener rol administrador.
- **FR-023**: La cuenta administrador DEBE preexistir sembrada en un archivo JSON local y NO DEBE poder crearse por registro.
- **FR-024**: Las cuentas de usuario DEBEN persistirse entre sesiones y sus contraseñas DEBEN almacenarse de forma segura (hash, nunca texto plano).
- **FR-025**: La sesión autenticada DEBE identificar el rol del usuario y DEBE restringir las operaciones administrativas al rol administrador.
- **FR-026**: Las capas temáticas DEBEN iniciar desactivadas y su alternancia DEBE respetar siempre el último estado solicitado por el usuario.
- **FR-027**: Los filtros del mismo atributo DEBEN combinarse con OR y los de atributos diferentes con AND; cambiar de capa DEBE limpiar los filtros anteriores.
- **FR-028**: Los resultados filtrados DEBEN listarse por entidad y permitir enfocar la geometría seleccionada en el mapa.
- **FR-029**: La selección de coordenadas desde el mapa DEBE estar ligada explícitamente al formulario que la solicita; la consulta puntual automática solo opera en su módulo.
- **FR-030**: El análisis y la comparación DEBEN representar centros y radios, reemplazar la visualización previa y ajustar el viewport a las zonas analizadas.
- **FR-031**: La comparación DEBE reutilizar el análisis de zona para presentar indicadores equivalentes, nombres y distancias exclusivamente de entidades dentro o intersectadas por cada radio, distinguiendo cero, ausencia de atributo y dataset no disponible.
- **FR-032**: Administración DEBE aceptar solo atributos descriptivos permitidos y tipos escalares observados en el dataset; los metadatos técnicos del proveedor y campos sin tipo observable NO DEBEN capturarse manualmente.
- **FR-033**: La geometría GEOINSIGHT DEBE poder dibujarse en el mapa de acuerdo con el tipo principal del dominio y continuar validándose en backend.
- **FR-034**: Administración DEBE mantener visibles las entidades GEOINSIGHT existentes, diferenciarlas por dominio y confirmar su eliminación mediante un modal de la aplicación.
- **FR-035**: La ayuda DEBE explicar cada módulo y mostrar contenido administrativo únicamente al rol ADMIN.
- **FR-036**: La pantalla de acceso DEBE ofrecer exclusivamente inicio de sesión y registro local, cargar su logotipo e imagen desde recursos accesibles sin sesión y NO DEBE presentar una opción SSO no soportada.
- **FR-037**: La navegación autenticada DEBE exponer solo controles funcionales; la ayuda se accede desde su módulo y la identidad de sesión se muestra una sola vez en el bloque inferior del menú, sin barra de búsqueda global no implementada.
- **FR-038**: Los campos de longitud y latitud para zona y comparación DEBEN iniciar vacíos con ejemplos de formato, y poder completarse tanto por teclado como mediante el selector explícito del mapa.
- **FR-039**: El detalle de una entidad DEBE adaptar la distribución de etiquetas y valores al ancho disponible para conservar su legibilidad.
- **FR-040**: El control flotante de capas DEBE ubicarse en la esquina inferior derecha del mapa, ser compacto, combinar el icono convencional de capas apiladas con la etiqueta “Capas” y mantener tooltip, etiqueta accesible y estados de interacción sin alterar su operación.
- **FR-041**: El selector de capas DEBE abrirse hacia arriba sobre el control, contener la misma lista de capas por dominio y DEBE cerrarse con un clic fuera del control o con la tecla Escape; la herramienta lateral “Explorar mapa” NO DEBE existir porque el control flotante es el único punto de activación de capas.
- **FR-042**: El panel contextual DEBE iniciar colapsado con «Buscar y filtrar» como módulo predeterminado; al expandirse NO DEBE mostrar un panel vacío y el estado visual del botón de colapso DEBE coincidir con el panel.
- **FR-043**: La consulta por coordenada DEBE presentar los resultados en tres secciones diferenciadas (Resultado, Contexto geológico y Elementos cercanos), cada dominio en una tarjeta compacta, sin mezclar contenedores con elementos próximos.
- **FR-044**: Las distancias DEBEN mostrarse en metros con un decimal para distancias menores a 1 km y en kilómetros con un decimal para distancias de 1 km o más, con separador decimal en español.
- **FR-045**: La consulta por coordenada DEBE marcar la ubicación consultada con un símbolo propio distinto de las entidades, resaltar las geometrías contenedoras y los elementos más cercanos (falla, movimiento en masa y volcán) y ajustar la vista al consultar desde el formulario para que queden visibles todos los elementos resaltados, sin superar un zoom máximo que conserve el contexto; el clic en el mapa NO DEBE alterar bruscamente la vista.
- **FR-046**: Los resultados DEBEN priorizar los nombres descriptivos del dataset y mostrar los identificadores técnicos como información secundaria; la ausencia de información por dominio DEBE presentarse con mensajes legibles y no inferir riesgo, amenaza o peligrosidad (FR-012).
- **FR-047**: Las capas temáticas y los resultados de zona y comparación DEBEN mostrar una vista previa de la entidad al pasar el cursor (nombre, procedencia y atributos principales), sin reemplazar el detalle completo que el clic fija en el panel.
- **FR-048**: La consulta por coordenada, el análisis de zona y la comparación DEBEN ofrecer una acción «Borrar» que restablezca el panel a su estado vacío y elimine del mapa los marcadores, círculos, entidades y resaltados propios del análisis, conservando las capas temáticas activas.
- **FR-049**: La consulta puntual DEBE limitarse a una cobertura seleccionada por disponibilidad: dominios tectónicos; en su ausencia, unidades geológicas; en ausencia de ambos, el borde local del basemap de Colombia. Fuera de esa cobertura DEBE responder `insideCoverage=false` sin calcular entidades cercanas. Esta restricción NO DEBE recortar el análisis ni la comparación por radio.

### Key Entities *(include if feature involves data)*

- **Entidad Geocientífica**: concepto común de los cinco dominios con procedencia (SGC o GEOINSIGHT), geometría y atributos descriptivos.
- **Movimiento en masa**: registro con tipo, subtipo y clasificación observables en el dataset SGC.
- **Falla geológica**: registro con nombre y tipo observables en el dataset SGC.
- **Unidad geológica**: registro con símbolo, edad, unidad y descripción observables en el dataset SGC.
- **Dominio tectónico**: registro con código y nombre observables en el dataset SGC.
- **Volcán**: registro con nombre, altura y atributos descriptivos observables en el dataset SGC.
- **Consulta por coordenada**: resultado integrado por dominio para una ubicación.
- **Zona de análisis**: coordenada central y radio, con indicadores descriptivos calculados.
- **Comparación de zonas**: dos zonas analizadas con los mismos criterios, presentadas lado a lado.
- **Cuenta de usuario**: credenciales de acceso (usuario, contraseña cifrada) y rol (usuario o administrador), con cuentas registradas siempre con rol usuario.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El usuario puede abrir el mapa, ver los cinco dominios disponibles y activar cualquiera de sus capas en menos de 10 segundos en una máquina local.
- **SC-002**: Una consulta por coordenada devuelve el contexto completo en menos de 5 segundos.
- **SC-003**: Un análisis de zona devuelve los indicadores en menos de 5 segundos.
- **SC-004**: La comparación muestra los mismos indicadores lado a lado para ambas zonas.
- **SC-005**: Ningún resultado del sistema contiene una conclusión de riesgo, amenaza o seguridad.
- **SC-006**: El 100% de los registros SGC permanecen sin modificar tras cualquier operación.
- **SC-007**: El 100% de los filtros y búsquedas utilizan atributos que existen realmente en el dataset.
- **SC-008**: El 100% de los accesos requieren autenticación y ninguna cuenta registrada obtiene rol administrador.
- **SC-009**: El 100% de las altas y ediciones administrativas rechazan atributos fuera de lista y valores con tipo incompatible.
- **SC-010**: Los flujos de zona, comparación y administración permiten capturar coordenadas o geometrías desde el mapa sin activar herramientas ajenas.

## Assumptions

- Es una aplicación web con backend y frontend, de uso local y de un solo usuario activo; no se requieren métricas de concurrencia ni escalabilidad.
- Todo acceso exige autenticación. La cuenta administrador está sembrada en un archivo JSON local de configuración; los usuarios de consulta pueden registrarse y nunca obtienen rol administrador.
- Los datasets son los cinco archivos GeoJSON del SGC obtenidos de las APIs REST oficiales (ArcGIS Feature Server) y guardados en `docs/datasets/`.
- Al iniciar, el sistema verifica que los cinco datasets existan y estén completos; si faltan o están incompletos, los descarga automáticamente (requiere conexión a internet únicamente la primera vez). Este bootstrap es infraestructura, no lógica de dominio.
- La geometría principal de cada dominio (punto, línea, polígono) es la indicada en el contexto funcional; las variantes multiparte se confirmarán al inspeccionar los archivos completos.
- Los diccionarios de datos definitivos (campos, tipos, nulabilidad, dominios de valores) se derivan de los datasets completos durante SDD.
- El sistema opera con datos geográficos locales tras el primer arranque; la integración con servicios web externos se limita al bootstrap de descarga de datasets y a los tiles del mapa base.
- La interfaz adapta sus paneles al espacio disponible; una experiencia móvil dedicada permanece fuera de alcance.
- Fuera de alcance: multiusuario concurrente, exportación de datos y edición de datasets SGC.
