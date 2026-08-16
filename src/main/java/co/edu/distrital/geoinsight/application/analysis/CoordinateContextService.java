package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * US3 — Consulta del contexto geocientífico de una coordenada (FR-009).
 * Semántica (research.md §5): contenedores por ray casting; vecino más cercano
 * por distancia mínima a la geometría; empates por orden de inserción estable.
 */
@Service
public class CoordinateContextService {

    private final EntityCatalog catalog;

    public CoordinateContextService(EntityCatalog catalog) {
        this.catalog = catalog;
    }

    public CoordinateContext context(Coordinate coordinate) {
        List<GeoscienceEntity> units = containing(Domain.UNIDAD_GEOLOGICA, coordinate);
        List<GeoscienceEntity> domains = containing(Domain.DOMINIO_TECTONICO, coordinate);
        return new CoordinateContext(coordinate, units, domains,
                nearest(Domain.FALLA_GEOLOGICA, coordinate),
                nearest(Domain.MOVIMIENTO_EN_MASA, coordinate),
                nearest(Domain.VOLCAN, coordinate));
    }

    private List<GeoscienceEntity> containing(Domain domain, Coordinate coordinate) {
        return catalog.findByDomain(domain).stream()
                .filter(entity -> entity.geometry().contains(coordinate))
                .toList();
    }

    private NearestEntity nearest(Domain domain, Coordinate coordinate) {
        NearestEntity nearest = null;
        for (GeoscienceEntity entity : catalog.findByDomain(domain)) {
            double distance = entity.geometry().distanceMeters(coordinate);
            if (nearest == null || distance < nearest.distanceMeters()) {
                nearest = new NearestEntity(entity, distance);
            }
        }
        return nearest;
    }
}
