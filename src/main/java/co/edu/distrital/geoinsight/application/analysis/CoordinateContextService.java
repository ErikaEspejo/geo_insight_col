package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.repository.CountryBoundary;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Comparator;

/**
 * US3 — Consulta del contexto geocientífico de una coordenada (FR-009).
 * Semántica (research.md §5): contenedores por ray casting; vecino más cercano
 * por distancia mínima a la geometría; empates por identificador ascendente.
 */
@Service
public class CoordinateContextService {

    private final EntityCatalog catalog;
    private final DatasetRepository datasetRepository;
    private final CountryBoundary countryBoundary;

    public CoordinateContextService(EntityCatalog catalog, DatasetRepository datasetRepository,
                                    CountryBoundary countryBoundary) {
        this.catalog = catalog;
        this.datasetRepository = datasetRepository;
        this.countryBoundary = countryBoundary;
    }

    public CoordinateContext context(Coordinate coordinate) {
        List<GeoscienceEntity> units = containing(Domain.UNIDAD_GEOLOGICA, coordinate);
        List<GeoscienceEntity> domains = containing(Domain.DOMINIO_TECTONICO, coordinate);
        boolean insideCoverage = insideCoverage(coordinate, units, domains);
        if (!insideCoverage) {
            return new CoordinateContext(coordinate, false, List.of(), List.of(), null, null, null);
        }
        return new CoordinateContext(coordinate, true, units, domains,
                nearest(Domain.FALLA_GEOLOGICA, coordinate),
                nearest(Domain.MOVIMIENTO_EN_MASA, coordinate),
                nearest(Domain.VOLCAN, coordinate));
    }

    private boolean insideCoverage(Coordinate coordinate, List<GeoscienceEntity> units,
                                   List<GeoscienceEntity> domains) {
        if (datasetRepository.isDatasetLoaded(Domain.DOMINIO_TECTONICO)) return !domains.isEmpty();
        if (datasetRepository.isDatasetLoaded(Domain.UNIDAD_GEOLOGICA)) return !units.isEmpty();
        return countryBoundary.contains(coordinate);
    }

    private List<GeoscienceEntity> containing(Domain domain, Coordinate coordinate) {
        return catalog.findByDomain(domain).stream()
                .filter(entity -> entity.geometry().contains(coordinate))
                .toList();
    }

    private NearestEntity nearest(Domain domain, Coordinate coordinate) {
        return catalog.findByDomain(domain).stream()
                .map(entity -> new NearestEntity(entity, entity.geometry().distanceMeters(coordinate)))
                .min(Comparator.comparingDouble(NearestEntity::distanceMeters)
                        .thenComparing(nearest -> nearest.entity().id()))
                .orElse(null);
    }
}
