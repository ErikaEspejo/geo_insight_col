package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.model.Zone;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * US5 — Comparación descriptiva de dos zonas con los mismos criterios
 * (FR-011, SC-004).
 */
@Service
public class ZoneComparisonService {

    private final ZoneAnalysisService zoneAnalysisService;
    private final CoordinateContextService coordinateContextService;

    public ZoneComparisonService(ZoneAnalysisService zoneAnalysisService,
                                 CoordinateContextService coordinateContextService) {
        this.zoneAnalysisService = zoneAnalysisService;
        this.coordinateContextService = coordinateContextService;
    }

    public ZoneComparisonResult compare(Zone zoneA, Zone zoneB) {
        Objects.requireNonNull(zoneA, "zona A requerida");
        Objects.requireNonNull(zoneB, "zona B requerida");
        return new ZoneComparisonResult(compareZone(zoneA), compareZone(zoneB));
    }

    private ComparedZone compareZone(Zone zone) {
        return new ComparedZone(
                zoneAnalysisService.analyze(zone),
                coordinateContextService.context(zone.center()));
    }
}
