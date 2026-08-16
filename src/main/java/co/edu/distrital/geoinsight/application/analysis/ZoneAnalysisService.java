package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Zone;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * US4 — Análisis descriptivo de una zona (FR-010). Semántica (research.md §5):
 * puntos dentro si distancia al centro ≤ radio; fallas si distancia a la línea
 * ≤ radio; polígonos presentes si contienen el centro o su distancia al centro
 * ≤ radio. Solo conteos, distribuciones y listados; sin riesgo (FR-012).
 */
@Service
public class ZoneAnalysisService {

    private final EntityCatalog catalog;

    public ZoneAnalysisService(EntityCatalog catalog) {
        this.catalog = catalog;
    }

    public ZoneAnalysisResult analyze(Zone zone) {
        List<GeoscienceEntity> movements = withinRadius(Domain.MOVIMIENTO_EN_MASA, zone);
        List<GeoscienceEntity> volcanoes = withinRadius(Domain.VOLCAN, zone);
        List<GeoscienceEntity> faults = withinRadius(Domain.FALLA_GEOLOGICA, zone);
        List<GeoscienceEntity> units = overlapping(Domain.UNIDAD_GEOLOGICA, zone);
        List<GeoscienceEntity> tectonicDomains = overlapping(Domain.DOMINIO_TECTONICO, zone);
        return new ZoneAnalysisResult(zone,
                ZoneBreakdown.movements(movements, catalog.dataAvailable(Domain.MOVIMIENTO_EN_MASA)),
                ZoneBreakdown.plain(faults, catalog.dataAvailable(Domain.FALLA_GEOLOGICA)),
                ZoneBreakdown.plain(units, catalog.dataAvailable(Domain.UNIDAD_GEOLOGICA)),
                ZoneBreakdown.plain(tectonicDomains, catalog.dataAvailable(Domain.DOMINIO_TECTONICO)),
                ZoneBreakdown.plain(volcanoes, catalog.dataAvailable(Domain.VOLCAN)));
    }

    private List<GeoscienceEntity> withinRadius(Domain domain, Zone zone) {
        return catalog.findByDomain(domain).stream()
                .filter(entity -> entity.geometry().distanceMeters(zone.center()) <= zone.radiusMeters())
                .toList();
    }

    private List<GeoscienceEntity> overlapping(Domain domain, Zone zone) {
        Coordinate center = zone.center();
        return catalog.findByDomain(domain).stream()
                .filter(entity -> entity.geometry().contains(center)
                        || entity.geometry().distanceMeters(center) <= zone.radiusMeters())
                .toList();
    }
}
