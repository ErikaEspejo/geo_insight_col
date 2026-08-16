# Interpretación del desarrollo realizado con ayuda de IA

## 1. Propósito de este documento

Este documento explica qué se construyó en GeoInsight Colombia, de qué manera se utilizó asistencia de inteligencia artificial y cómo se verificó que el resultado correspondiera con los datos, la guía académica y el comportamiento real del código. No reemplaza la especificación del sistema; ofrece una lectura crítica del proceso de construcción.

## 2. Interpretación de la solución

GeoInsight Colombia transforma cinco conjuntos de datos abiertos del Servicio Geológico Colombiano en una aplicación web local de exploración y análisis descriptivo. La plataforma permite visualizar capas, filtrar atributos reales, consultar el contexto de una coordenada, analizar zonas circulares, comparar dos zonas y administrar entidades propias sin modificar los registros oficiales.

La intención central no es predecir eventos ni establecer niveles de riesgo. El sistema organiza información geocientífica heterogénea y la hace consultable mediante reglas explícitas de distancia, contención e intersección. Toda salida debe interpretarse como descripción de los registros disponibles, no como diagnóstico técnico sobre seguridad, amenaza o vulnerabilidad.

## 3. Uso de la inteligencia artificial

La IA se utilizó como herramienta de apoyo para:

- convertir el contexto inicial y las decisiones del proyecto en una especificación estructurada;
- proponer una organización por capas y responsabilidades orientadas a objetos;
- redactar historias de usuario, criterios de aceptación y casos límite;
- ayudar a implementar clases, servicios, controladores, persistencia JSON y frontend;
- proponer pruebas y revisar la trazabilidad entre requisitos, código y validación;
- mejorar la redacción de la documentación y generar fuentes de diagramas PlantUML.

Las respuestas de la IA no se asumieron automáticamente como correctas. Cada propuesta relevante se contrastó con los archivos GeoJSON reales, el código compilado, las reglas del proyecto y las pruebas automatizadas. Cuando la IA sugería campos, geometrías o comportamientos que no estaban sustentados, se debía conservar como autoridad la especificación canónica y la evidencia observada en los datasets.

## 4. Decisiones técnicas interpretadas

### Dominio independiente

Las clases del dominio no dependen de Spring, Jackson, HTTP, JSON ni Leaflet. Esta separación permite expresar y probar reglas como coordenadas válidas, radios positivos, distancia a geometrías y procedencia de entidades sin levantar el servidor web.

### Herencia limitada a geometrías

La herencia se usa en la jerarquía `Geometry` porque `Point`, `LineString`, `Polygon` y sus variantes multiparte son especializaciones reales con comportamientos polimórficos. Los cinco dominios geocientíficos no se convirtieron en subclases artificiales: comparten la estructura `GeoscienceEntity` y varían mediante datos, enumeraciones y metadatos.

### Composición y servicios de aplicación

Los casos de uso coordinan repositorios y objetos del dominio mediante composición. `EntityCatalog` unifica registros SGC y GEOINSIGHT; los servicios de contexto, zona y comparación reutilizan esa abstracción sin trasladar reglas al controlador o al navegador.

### Persistencia y alcance local

La persistencia usa JSON local detrás de interfaces de repositorio. Esta decisión simplifica la ejecución académica y cumple la restricción de no introducir una base de datos. Su desventaja es que no está diseñada para concurrencia elevada ni volúmenes mucho mayores.

### Datos completos frente a visualización simplificada

Los cálculos utilizan geometrías completas. La simplificación Douglas-Peucker se aplica únicamente al GeoJSON enviado para visualizar polígonos pesados. Así se mejora la respuesta gráfica sin alterar contención, distancia o intersección en el análisis.

## 5. Validación del resultado

La validación combina pruebas de dominio, servicios, infraestructura, seguridad, contrato web y rendimiento. La ejecución más reciente de `mvnw.cmd clean verify` compiló 75 archivos Java de producción y 21 archivos de prueba, y finalizó con 101 pruebas aprobadas, sin fallos, errores ni omisiones.

Además de la automatización, la documentación conserva criterios de aceptación manual para login, capas, consultas, análisis, comparación, administración y operación sin conexión. Las cifras y afirmaciones del proyecto deben actualizarse cuando cambien el código o los datasets; un texto generado previamente por IA no constituye evidencia por sí mismo.

## 6. Aportes y limitaciones de la IA

El principal aporte de la IA fue acelerar la exploración de alternativas, la generación de borradores y la revisión cruzada entre muchos artefactos. También ayudó a hacer explícitas decisiones que de otro modo podían quedar solo en el código.

La principal limitación es que la IA puede producir requisitos inexistentes, confundir atributos, afirmar validaciones no ejecutadas o diseñar abstracciones innecesarias. Por esa razón, el proceso mantuvo cuatro controles: datos reales como fuente para el modelo, especificación como autoridad funcional, pruebas como evidencia ejecutable y revisión humana como responsabilidad final.

## 7. Estado final interpretado

El proyecto quedó como una aplicación local funcional y verificable, con separación por capas, diseño orientado a objetos, autenticación por sesión, frontend cartográfico, persistencia JSON y pruebas automatizadas. Su alcance es apropiado para demostrar conceptos de programación orientada a objetos y trabajo guiado por especificaciones. No pretende sustituir plataformas geocientíficas institucionales ni realizar análisis de riesgo.
