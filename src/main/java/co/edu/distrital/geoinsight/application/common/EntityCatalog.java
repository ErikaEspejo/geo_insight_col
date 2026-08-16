package co.edu.distrital.geoinsight.application.common;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import co.edu.distrital.geoinsight.domain.repository.GeoEntityRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vista unificada de entidades SGC (solo lectura) y GEOINSIGHT (editable),
 * para que las consultas y análisis integren ambas procedencias (FR-019).
 */
@Service
public class EntityCatalog {

    private final DatasetRepository datasetRepository;
    private final GeoEntityRepository geoEntityRepository;

    public EntityCatalog(DatasetRepository datasetRepository, GeoEntityRepository geoEntityRepository) {
        this.datasetRepository = datasetRepository;
        this.geoEntityRepository = geoEntityRepository;
    }

    public List<GeoscienceEntity> findByDomain(Domain domain) {
        List<GeoscienceEntity> entities = new ArrayList<>(datasetRepository.findSgcByDomain(domain));
        entities.addAll(geoEntityRepository.findByDomain(domain));
        return List.copyOf(entities);
    }

    public Optional<GeoscienceEntity> findById(String id) {
        return datasetRepository.findSgcById(id).or(() -> geoEntityRepository.findById(id));
    }

    public Map<Domain, Integer> counts() {
        Map<Domain, Integer> counts = new EnumMap<>(Domain.class);
        for (Domain domain : Domain.values()) {
            counts.put(domain, findByDomain(domain).size());
        }
        return counts;
    }

    public boolean dataAvailable(Domain domain) {
        return datasetRepository.isDatasetLoaded(domain) && !findByDomain(domain).isEmpty();
    }
}
