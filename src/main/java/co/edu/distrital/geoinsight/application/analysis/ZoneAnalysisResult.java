package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.model.Zone;

import java.util.Objects;

/**
 * Resultado del análisis descriptivo de una zona (FR-010).
 */
public record ZoneAnalysisResult(
        Zone zone,
        ZoneBreakdown massMovements,
        ZoneBreakdown faults,
        ZoneBreakdown geologicalUnits,
        ZoneBreakdown tectonicDomains,
        ZoneBreakdown volcanoes) {

    public ZoneAnalysisResult {
        Objects.requireNonNull(zone, "zona requerida");
        Objects.requireNonNull(massMovements, "movimientos requeridos");
        Objects.requireNonNull(faults, "fallas requeridas");
        Objects.requireNonNull(geologicalUnits, "unidades requeridas");
        Objects.requireNonNull(tectonicDomains, "dominios requeridos");
        Objects.requireNonNull(volcanoes, "volcanes requeridos");
    }
}
