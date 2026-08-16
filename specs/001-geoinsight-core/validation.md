# Validation Evidence: GeoInsight Core

Registro reproducible de aceptación. Una tarea visual solo puede marcarse
completa cuando incluye fecha, entorno y resultado observado.

## Validación automatizada

Comando: `mvn clean verify`.

Resultado final 2026-08-16: 100 pruebas ejecutadas, 0 fallos, 0 errores y 0
omitidas (`BUILD SUCCESS`).

`PerformanceAcceptanceTest` usa los cinco datasets reales y falla cuando se
exceden los presupuestos:

| Criterio | Operación automatizada | Presupuesto | Medición 2026-08-16 |
|---|---|---:|---:|
| SC-001 (servidor) | GeoJSON simplificado de 7461 unidades | < 10 s | 0.689 s |
| SC-002 | Contexto de coordenada | < 5 s | 0.072 s |
| SC-003 | Análisis de zona de 50 km | < 5 s | 0.558 s |

Los tiempos excluyen el arranque de Spring y no sustituyen la medición del
render del navegador exigida por SC-001.

## Evidencia de bootstrap FR-050/FR-051

- `SgcDatasetDownloaderTest.retriesAfterTransientHttpFailure` provoca un HTTP
  503 en el primer intento y verifica descarga correcta en el segundo.
- El arranque manual del 2026-08-16 no aceptó conexiones hasta terminar el
  bootstrap; la primera respuesta de `/api/layers` informó los conteos
  6826/4866/7461/3/61 con `dataAvailable=true` en los cinco dominios.
- Configuración verificada: 3 intentos totales y espera base de 1000 ms; la
  espera crece linealmente por número de intento.

## Validación visual y manual completada

Fecha: 2026-08-16.

Entorno: ejecución local de la aplicación en Windows con Java 21. La validación
manual fue realizada y confirmada por el usuario responsable del proyecto.

Resultados registrados:

1. Las capas temáticas comienzan apagadas.
2. El render completo de `Unidades geológicas` cumple el límite de SC-001
   (menos de 10 segundos) y la aplicación conserva capacidad de interacción.
3. Los escenarios E1–E9 de `quickstart.md` fueron ejecutados satisfactoriamente.
4. La prueba sin conexión funciona con el fondo vectorial local y los datasets
   previamente descargados.
5. Los flujos visuales, controles por rol, limpieza de análisis y herramientas
   cartográficas se comportan conforme a sus criterios de aceptación.

Estado 2026-08-16: validación completada. T047b y T048-R quedan cerradas con
evidencia manual aportada por el usuario y con la evidencia automatizada de la
sección anterior.
