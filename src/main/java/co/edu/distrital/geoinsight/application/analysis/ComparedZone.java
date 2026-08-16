package co.edu.distrital.geoinsight.application.analysis;

import java.util.Objects;

/** Indicadores de radio y contexto puntual equivalentes para un lado de la comparación. */
public record ComparedZone(ZoneAnalysisResult analysis, CoordinateContext centerContext) {

    public ComparedZone {
        Objects.requireNonNull(analysis, "análisis requerido");
        Objects.requireNonNull(centerContext, "contexto central requerido");
    }
}
