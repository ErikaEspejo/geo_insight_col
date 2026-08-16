package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.model.Zone;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Comparator;
import java.util.List;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

/**
 * US5 — Comparación descriptiva de dos zonas con los mismos criterios
 * (FR-011, SC-004).
 */
@Service
public class ZoneComparisonService {

    private final ZoneAnalysisService zoneAnalysisService;

    public ZoneComparisonService(ZoneAnalysisService zoneAnalysisService) {
        this.zoneAnalysisService = zoneAnalysisService;
    }

    public ZoneComparisonResult compare(Zone zoneA, Zone zoneB) {
        Objects.requireNonNull(zoneA, "zona A requerida");
        Objects.requireNonNull(zoneB, "zona B requerida");
        return new ZoneComparisonResult(compareZone(zoneA), compareZone(zoneB));
    }

    private ComparedZone compareZone(Zone zone) {
        ZoneAnalysisResult analysis = zoneAnalysisService.analyze(zone);
        return new ComparedZone(
                analysis,
                nearest(analysis.faults().entities(), zone.center()),
                nearest(analysis.massMovements().entities(), zone.center()),
                nearest(analysis.volcanoes().entities(), zone.center()));
    }

    private NearestEntity nearest(List<GeoscienceEntity> entities, Coordinate center) {
        return entities.stream()
                .map(entity -> new NearestEntity(entity, entity.geometry().distanceMeters(center)))
                .min(Comparator.comparingDouble(NearestEntity::distanceMeters)
                        .thenComparing(nearest -> nearest.entity().id()))
                .orElse(null);
    }
}
