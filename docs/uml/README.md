# Índice de diagramas UML y PlantUML

Los diagramas se conservan como código PlantUML dentro de la documentación para que puedan revisarse, copiarse y volver a renderizarse. No son imágenes aisladas sin fuente editable.

## Diseño orientado a objetos

- [Diagrama de clases del núcleo](../../specs/001-geoinsight-core/design.md#diagrama-de-clases-del-núcleo): jerarquía `Geometry`, entidades, repositorios, servicios e implementaciones de infraestructura.
- [Decisiones POO](../../specs/001-geoinsight-core/design.md#decisiones-poo-demostradas): justificación de abstracción, encapsulamiento, herencia, polimorfismo y composición.

## Diagramas consolidados del sistema

La [especificación integral](../ESPECIFICACION_DEL_SISTEMA.md#10-arquitectura) contiene las fuentes PlantUML de:

1. diagrama de contexto;
2. contenedores y componentes;
3. clases del núcleo;
4. despliegue;
5. estados de acceso;
6. secuencia de arranque y disponibilidad;
7. secuencia de autenticación;
8. secuencia de exploración y filtrado;
9. secuencia de consulta por coordenada;
10. secuencia de análisis y comparación de zonas;
11. secuencia de gestión administrativa.

## Renderizado opcional

Cada bloque comprendido entre `@startuml` y `@enduml` puede guardarse como archivo `.puml` y renderizarse con PlantUML. La fuente textual es el artefacto incluido en la entrega; el render es opcional y no modifica el diseño documentado.
