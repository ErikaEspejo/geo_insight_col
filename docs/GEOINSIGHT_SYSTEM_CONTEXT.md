# GeoInsight Colombia

## Contexto funcional consolidado para iniciar SDD

**Estado del documento:** contexto inicial del sistema  
**Propósito:** servir como entrada para el proceso de Desarrollo Dirigido por Especificaciones (SDD)  
**Alcance:** definición funcional, reglas y evidencia parcial de datos  

> Este documento establece qué sistema se desarrollará y qué decisiones ya están acordadas. No define clases, arquitectura, implementación ni contratos definitivos. Los contratos, tipos, restricciones y campos finales deberán derivarse de la inspección programática de los datasets completos durante SDD.

## 1. Definición del sistema

**GeoInsight Colombia** es un sistema de información geocientífica que integra información abierta del Servicio Geológico Colombiano (SGC) con información incorporada directamente en GeoInsight.

Su finalidad es permitir que una persona explore información geocientífica de Colombia, consulte el contexto de una coordenada, caracterice una zona y compare descriptivamente dos zonas.

GeoInsight trabaja con cinco dominios:

| Dominio | Dataset o capa de referencia | Geometría principal conocida |
|---|---|---|
| Movimientos en masa | Inventario de movimientos en masa del SGC | `Point` |
| Fallas geológicas | Fallas geológicas del SGC | `LineString` |
| Unidades geológicas | Unidades geológicas del Mapa Geológico de Colombia 2015, escala 1:1.000.000 | `Polygon` |
| Dominios tectónicos | Dominios o unidades tectónicas del SGC | `Polygon` |
| Volcanes | Volcanes del SGC | `Point` |

La geometría principal indicada corresponde a las muestras y al uso funcional acordado. La presencia de variantes como `MultiPoint`, `MultiLineString` o `MultiPolygon` no debe aceptarse ni descartarse hasta revisar los archivos completos.

## 2. Objetivo funcional

GeoInsight debe permitir:

- explorar las entidades de los cinco dominios;
- visualizarlas como capas sobre un mapa;
- activar y desactivar capas;
- seleccionar una entidad y consultar sus atributos;
- buscar y filtrar entidades mediante atributos que existan realmente en cada dataset;
- consultar el contexto geocientífico de una coordenada;
- analizar una zona definida por una coordenada central y un radio;
- comparar descriptivamente dos zonas;
- distinguir de forma inequívoca los registros oficiales del SGC de los registros creados en GeoInsight;
- permitir al administrador incorporar entidades propias en cualquiera de los cinco dominios;
- proteger los datos del SGC contra modificación y eliminación.

## 3. Actores y permisos

GeoInsight tiene exactamente dos actores funcionales.

### 3.1 Usuario

El usuario normal utiliza el sistema exclusivamente para consulta y análisis. Puede:

- explorar el mapa y las capas;
- consultar entidades y sus atributos;
- buscar y filtrar información;
- consultar una coordenada;
- analizar una zona;
- comparar dos zonas;
- visualizar los resultados.

El usuario normal no crea, edita ni elimina información geocientífica.

### 3.2 Administrador

El administrador puede realizar todas las operaciones de consulta y análisis del usuario normal. Además, es el único actor autorizado para:

- crear entidades de cualquiera de los cinco dominios;
- editar entidades cuyo origen sea `GEOINSIGHT`;
- eliminar entidades cuyo origen sea `GEOINSIGHT`.

El administrador no puede editar ni eliminar entidades cuyo origen sea `SGC`.

### 3.3 Matriz consolidada de permisos

| Acción | Usuario | Administrador |
|---|:---:|:---:|
| Explorar mapa y capas | Sí | Sí |
| Consultar entidades | Sí | Sí |
| Buscar y filtrar | Sí | Sí |
| Consultar coordenada | Sí | Sí |
| Analizar una zona | Sí | Sí |
| Comparar dos zonas | Sí | Sí |
| Crear una entidad | No | Sí |
| Editar una entidad `GEOINSIGHT` | No | Sí |
| Eliminar una entidad `GEOINSIGHT` | No | Sí |
| Editar una entidad `SGC` | No | No |
| Eliminar una entidad `SGC` | No | No |

No existe en el alcance acordado un flujo en el que un usuario normal reporte información para posterior aprobación. La incorporación de datos es responsabilidad exclusiva del administrador.

## 4. Procedencia e inmutabilidad

Cada entidad debe conservar su procedencia mediante uno de estos valores conceptuales:

- `SGC`: registro proveniente de un dataset oficial del Servicio Geológico Colombiano;
- `GEOINSIGHT`: registro incorporado dentro del sistema por un administrador autorizado.

Las reglas son:

1. Los registros `SGC` son información de referencia y siempre son de solo lectura.
2. Los registros `SGC` no se editan ni se eliminan desde GeoInsight.
3. Los registros `GEOINSIGHT` pueden ser editados o eliminados únicamente por un administrador.
4. Un registro creado dentro del sistema debe conservar su origen `GEOINSIGHT`; no debe presentarse como información oficial del SGC.
5. La procedencia debe ser visible o recuperable en las consultas de la entidad.

La posibilidad de crear una falla, un volcán, una unidad geológica o un dominio tectónico no significa que el sistema valide científicamente una entidad inventada. Significa que permite registrar información proveniente de otra fuente o de un levantamiento posterior. La validez de esa información recae en el actor autorizado que la incorpora.

## 5. Exploración y visualización cartográfica

El mapa es una parte central de la interacción. Debe permitir:

- mostrar los cinco dominios como capas diferenciadas;
- activar o desactivar cada capa;
- representar cada entidad conforme a su geometría;
- seleccionar entidades y consultar sus atributos;
- indicar o seleccionar una coordenada para consulta;
- representar el centro y el radio de una zona de análisis;
- representar las dos zonas utilizadas en una comparación.

Representación conceptual:

| Dominio | Representación espacial |
|---|---|
| Movimiento en masa | Punto |
| Volcán | Punto |
| Falla geológica | Línea |
| Unidad geológica | Polígono |
| Dominio tectónico | Polígono |

El mapa apoya la interacción y la presentación de resultados. No reemplaza la lógica que determina contención, proximidad, presencia o agregaciones.

## 6. Consulta y filtrado por atributos

Cada dominio debe poder consultarse y filtrarse mediante atributos que existan realmente en su fuente. Los filtros definitivos no se deben inventar: se establecerán al construir los diccionarios de datos durante SDD.

Como intención funcional inicial se conocen estas categorías:

| Dominio | Criterios candidatos, sujetos a verificación |
|---|---|
| Movimientos en masa | tipo, subtipo y clasificación |
| Fallas geológicas | nombre y tipo |
| Unidades geológicas | símbolo, edad, unidad y descripción |
| Dominios tectónicos | código y nombre del dominio |
| Volcanes | nombre, altura y demás atributos descriptivos disponibles |

Que un atributo sea candidato a filtro no implica que sea obligatorio, completo, único ni que tenga un tipo determinado.

## 7. Consulta por coordenada

El usuario puede introducir una coordenada geográfica o seleccionar un punto en el mapa. GeoInsight debe generar una consulta integrada que responda:

> ¿Qué contexto geocientífico tiene esta ubicación?

La respuesta puede incluir, según la disponibilidad y cobertura de los datos:

### Unidades geológicas

- unidad geológica que contiene el punto;
- símbolo, edad y descripción disponibles de esa unidad.

### Dominios tectónicos

- dominio tectónico que contiene el punto;
- atributos descriptivos disponibles del dominio.

### Fallas geológicas

- falla más cercana;
- distancia desde la coordenada hasta esa falla;
- atributos descriptivos disponibles.

### Movimientos en masa

- registro más cercano;
- distancia hasta ese registro;
- registros existentes en un entorno cuando la operación use un radio explícito.

### Volcanes

- volcán más cercano;
- distancia hasta ese volcán;
- atributos descriptivos disponibles.

Los comportamientos ante ausencia de resultados, superposición de polígonos, empates de proximidad o geometrías inválidas deben decidirse y formalizarse durante SDD.

## 8. Análisis de una zona

El usuario define:

- una coordenada central;
- un radio de análisis.

GeoInsight construye una caracterización descriptiva del entorno. Según lo que permitan los datos y las decisiones de especificación, puede incluir:

### Movimientos en masa

- cantidad de registros dentro de la zona;
- densidad de registros, si se formaliza la unidad de área y el método de cálculo;
- distribución por tipo;
- distribución por subtipo;
- tipo predominante;
- movimiento más cercano al centro.

### Fallas geológicas

- fallas presentes o próximas a la zona;
- cantidad de fallas según el criterio espacial definido;
- falla más cercana al centro;
- distancia a la falla más cercana.

### Unidades geológicas

- unidad que contiene el punto central;
- unidades presentes dentro de la zona;
- edades y descripciones asociadas disponibles.

### Dominios tectónicos

- dominio que contiene el punto central;
- dominios presentes en la zona, cuando corresponda.

### Volcanes

- volcán más cercano al centro;
- distancia al volcán más cercano;
- volcanes ubicados dentro de la zona o de otro entorno explícitamente definido.

SDD debe precisar qué significa “presente”, “próximo” y “dentro de la zona” para puntos, líneas y polígonos, así como el tratamiento de entidades que intersectan parcialmente el límite.

## 9. Comparación descriptiva de dos zonas

El usuario selecciona dos coordenadas y un radio común de análisis. GeoInsight analiza ambas zonas con los mismos criterios y presenta indicadores equivalentes lado a lado.

La comparación puede contrastar, entre otros resultados definidos durante SDD:

- unidades geológicas centrales o presentes;
- dominios tectónicos centrales o presentes;
- cantidad y distribución de movimientos en masa;
- tipos predominantes;
- fallas presentes;
- falla más cercana y su distancia;
- volcán más cercano y su distancia.

La comparación es estrictamente descriptiva. Puede afirmar, por ejemplo:

> La zona A contiene una mayor cantidad de movimientos en masa registrados que la zona B dentro del radio analizado.

No puede concluir que una zona es más peligrosa, más segura o tiene mayor riesgo.

## 10. Creación y gestión de entidades `GEOINSIGHT`

El administrador puede crear entidades en cualquiera de los dominios, respetando la geometría admitida por ese dominio:

| Dominio | Geometría funcional esperada para creación |
|---|---|
| Movimiento en masa | Punto |
| Volcán | Punto |
| Falla geológica | Línea |
| Unidad geológica | Polígono |
| Dominio tectónico | Polígono |

Los campos obligatorios, validaciones, dominios de valores y reglas geométricas de cada formulario no están definidos todavía. Deben derivarse de:

1. el esquema completo del dataset correspondiente;
2. la calidad y nulabilidad observadas;
3. las necesidades mínimas para que una entidad creada en GeoInsight pueda consultarse, visualizarse y analizarse correctamente.

Una entidad `GEOINSIGHT` puede participar en consultas y análisis. SDD deberá hacer explícito si los resultados se agregan conjuntamente o se presentan también desglosados por procedencia.

## 11. Evidencia parcial conocida de los datasets

La conversación incluyó muestras parciales, no los archivos completos. Esta sección registra únicamente la evidencia conservada y las categorías mencionadas; no constituye un diccionario de datos.

### 11.1 Movimientos en masa

En las muestras se observaron literalmente estos atributos:

- `TIPO`;
- `SUBTIPO`;
- `CLAS_MAPA`.

La geometría observada o esperada para la capa es `Point`.

Todavía no se conocen:

- todos los valores posibles de `TIPO`, `SUBTIPO` y `CLAS_MAPA`;
- la frecuencia de valores nulos o vacíos;
- la relación válida entre tipo y subtipo;
- el conjunto total de campos;
- si todos los registros usan exclusivamente `Point`.

### 11.2 Fallas geológicas

En una muestra se observó literalmente:

- `NombreFalla: null`.

También se identificaron nombre y tipo como categorías candidatas de consulta, pero el nombre exacto del campo de tipo debe confirmarse en el dataset.

La geometría observada o esperada es `LineString`.

La muestra demuestra que no se debe declarar el nombre de una falla como obligatorio sin inspeccionar el conjunto completo. También debe verificarse si existen geometrías `MultiLineString`.

### 11.3 Unidades geológicas

La fuente acordada es la capa de unidades geológicas del **Mapa Geológico de Colombia 2015, escala 1:1.000.000**. Se han mencionado como categorías relevantes:

- símbolo o código;
- edad;
- unidad geológica;
- descripción.

Estos son conceptos funcionales conocidos, no una confirmación de los nombres literales de las columnas.

La geometría observada o esperada es `Polygon`. Debe comprobarse la posible presencia de `MultiPolygon`, anillos interiores, geometrías inválidas, solapamientos y vacíos de cobertura.

### 11.4 Dominios tectónicos

Se han mencionado como categorías relevantes:

- código;
- nombre del dominio.

Los nombres literales, tipos, nulabilidad y dominios de valores deben obtenerse del archivo completo.

La geometría observada o esperada es `Polygon`, sujeta a verificación de variantes multiparte.

### 11.5 Volcanes

En una muestra se observaron literalmente:

- `VolcanID: null`;
- `NombreVolcan: ""`.

También se mencionó la altura como atributo descriptivo candidato, pero su nombre literal y disponibilidad deben confirmarse.

La geometría observada o esperada es `Point`.

La muestra demuestra que no se debe asumir que el identificador o el nombre estén siempre informados, ni tratar una cadena vacía como un nombre válido sin definir primero una regla de normalización.

## 12. Límites explícitos

GeoInsight no calcula ni comunica:

- riesgo;
- amenaza;
- vulnerabilidad;
- peligrosidad;
- probabilidad de ocurrencia;
- seguridad de una zona;
- predicciones o recomendaciones de evacuación, construcción o uso del suelo.

La presencia, cantidad, densidad o proximidad de registros geocientíficos no equivale por sí sola a una medición de amenaza o riesgo.

El sistema tampoco debe crear clasificaciones interpretativas no respaldadas por una especificación y por los datos disponibles.

## 13. Decisiones que SDD debe derivar de los datasets completos

Antes de cerrar contratos del dominio, se deben inspeccionar los cinco archivos completos y determinar para cada uno:

- cantidad de entidades;
- sistema de referencia de coordenadas y orden de coordenadas;
- tipos de geometría realmente presentes;
- geometrías nulas, vacías o inválidas;
- lista completa de atributos;
- tipo observado de cada atributo;
- valores nulos, cadenas vacías y valores centinela;
- valores únicos y dominios de categorías;
- identificadores disponibles, duplicados y confiabilidad;
- campos técnicos que no representan conceptos del dominio;
- inconsistencias de formato, capitalización y codificación;
- cobertura espacial;
- significado de los atributos según los metadatos oficiales;
- reglas de normalización necesarias;
- atributos aptos para búsqueda, filtrado, presentación y análisis.

Con esa evidencia, SDD debe definir:

- diccionarios de datos definitivos;
- requisitos funcionales detallados;
- precondiciones y postcondiciones;
- validaciones de coordenadas, radios y geometrías;
- comportamiento ante datos faltantes o ambiguos;
- semántica exacta de contención, intersección, cercanía y distancia;
- reglas de creación de entidades `GEOINSIGHT`;
- criterios de agregación y comparación;
- criterios de aceptación verificables.

## 14. Reglas para evitar supuestos prematuros

Durante SDD no se debe:

- convertir ejemplos parciales en restricciones universales;
- asumir que un campo es obligatorio porque aparece diligenciado en una muestra;
- asumir que un identificador es único o confiable;
- reemplazar cadenas vacías por valores inventados;
- imponer enumeraciones antes de obtener todos los valores observados;
- asumir geometrías simples cuando podrían existir variantes multiparte;
- inventar filtros para atributos que no existen;
- confundir datos `GEOINSIGHT` con datos oficiales del SGC;
- formular conclusiones de riesgo o amenaza a partir de análisis descriptivos.

## 15. Síntesis del alcance congelado

GeoInsight Colombia integra cinco dominios geocientíficos del SGC y entidades propias de GeoInsight. Los usuarios normales solo exploran, consultan y analizan. El administrador es el único actor que crea información y solo puede editar o eliminar registros de origen `GEOINSIGHT`. Los datos `SGC` son inmutables. El sistema ofrece mapa por capas, filtros basados en atributos reales, consulta por coordenada, análisis de zona mediante coordenada y radio, y comparación descriptiva de dos zonas. No calcula riesgo ni amenaza. Los modelos, campos, restricciones y contratos definitivos se obtendrán de los datasets completos durante SDD.
