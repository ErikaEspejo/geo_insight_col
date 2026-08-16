# Validation Evidence: GeoInsight Core

Registro reproducible de aceptación. Una tarea visual solo puede marcarse
completa cuando incluye fecha, entorno y resultado observado.

## Validación automatizada

Comando: `mvn clean verify`.

`PerformanceAcceptanceTest` usa los cinco datasets reales y falla cuando se
exceden los presupuestos:

| Criterio | Operación automatizada | Presupuesto | Medición 2026-08-16 |
|---|---|---:|---:|
| SC-001 (servidor) | GeoJSON simplificado de 7461 unidades | < 10 s | 0.689 s |
| SC-002 | Contexto de coordenada | < 5 s | 0.072 s |
| SC-003 | Análisis de zona de 50 km | < 5 s | 0.558 s |

Los tiempos excluyen el arranque de Spring y no sustituyen la medición del
render del navegador exigida por SC-001.

## Validación visual pendiente

1. Ejecutar `mvn spring-boot:run` e iniciar sesión.
2. Confirmar que las capas temáticas comienzan apagadas.
3. Medir en las herramientas Performance del navegador desde el clic en
   `Unidades geológicas` hasta terminar su render. Debe ser menor a 10 s.
4. Registrar navegador, equipo, duración y capacidad de interacción.
5. Ejecutar E1–E9 de `quickstart.md`, incluida la prueba offline.

Estado 2026-08-16: no ejecutada porque el entorno de validación no expuso un
navegador controlable. T047b y T048-R permanecen abiertas; no se infiere su
cumplimiento desde las pruebas backend.
