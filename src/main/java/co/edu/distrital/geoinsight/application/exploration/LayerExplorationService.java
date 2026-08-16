package co.edu.distrital.geoinsight.application.exploration;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.GeometryKind;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * US2 — Exploración por capas. Expone metadatos de capa (atributos reales con
 * valores distintos) y consulta de entidades filtrada por atributos presentes
 * en el dataset (FR-008, FR-018).
 */
@Service
public class LayerExplorationService {

    private static final int MAX_DISTINCT_VALUES = 200;
    private static final int HEAVY_POLYGON_LAYER_THRESHOLD = 1000;

    private final EntityCatalog catalog;
    private final DatasetRepository datasetRepository;

    public LayerExplorationService(EntityCatalog catalog, DatasetRepository datasetRepository) {
        this.catalog = catalog;
        this.datasetRepository = datasetRepository;
    }

    /**
     * Transformación de geometría para VISUALIZACIÓN: capas de polígonos
     * pesadas se simplifican (Douglas-Peucker + redondeo) para que Leaflet
     * renderice en tiempo (contingencia SC-001). El análisis y el detalle
     * usan siempre la geometría completa.
     */
    public Function<Geometry, Geometry> visualizationTransform(Domain domain) {
        if (domain.geometryKind() == GeometryKind.POLYGON
                && catalog.findByDomain(domain).size() > HEAVY_POLYGON_LAYER_THRESHOLD) {
            return new GeometrySimplifier(0.01, 4)::simplify;
        }
        return Function.identity();
    }

    public List<LayerMetadata> layers() {
        List<LayerMetadata> layers = new ArrayList<>();
        for (Domain domain : Domain.values()) {
            Map<String, List<String>> attributes = new LinkedHashMap<>();
            for (String attribute : sortedAttributes(domain)) {
                attributes.put(attribute, distinctValues(domain, attribute));
            }
            layers.add(new LayerMetadata(domain, domain.displayName(),
                    geometryTypeName(domain.geometryKind()),
                    catalog.findByDomain(domain).size(), attributes,
                    datasetRepository.requiredAttributes(domain),
                    datasetRepository.editableAttributes(domain),
                    datasetRepository.editableAttributeTypes(domain),
                    dataAvailable(domain)));
        }
        return layers;
    }

    public List<GeoscienceEntity> entities(Domain domain, Map<String, String> filters) {
        Map<String, List<String>> groupedFilters = new LinkedHashMap<>();
        filters.forEach((attribute, value) -> groupedFilters.put(attribute, List.of(value)));
        return entitiesWithFilters(domain, groupedFilters);
    }

    public List<GeoscienceEntity> entitiesWithFilters(Domain domain, Map<String, List<String>> filters) {
        Set<String> realAttributes = datasetRepository.attributeNames(domain);
        for (String attribute : filters.keySet()) {
            if (!realAttributes.contains(attribute)) {
                throw new IllegalArgumentException(
                        "El atributo no existe en el dataset: " + attribute);
            }
        }
        return catalog.findByDomain(domain).stream()
                .filter(entity -> matchesFilters(entity, filters))
                .toList();
    }

    public Optional<GeoscienceEntity> entity(Domain domain, String id) {
        return catalog.findById(id).filter(entity -> entity.domain() == domain);
    }

    private boolean matchesFilters(GeoscienceEntity entity, Map<String, List<String>> filters) {
        for (Map.Entry<String, List<String>> filter : filters.entrySet()) {
            Optional<String> value = entity.attributeString(filter.getKey());
            if (value.isEmpty() || !filter.getValue().contains(value.get())) {
                return false;
            }
        }
        return true;
    }

    private boolean dataAvailable(Domain domain) {
        return !datasetRepository.missingDatasets().contains(domain)
                && !catalog.findByDomain(domain).isEmpty();
    }

    private List<String> sortedAttributes(Domain domain) {
        return datasetRepository.attributeNames(domain).stream()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private List<String> distinctValues(Domain domain, String attribute) {
        return datasetRepository.distinctValues(domain, attribute).stream()
                .map(String::valueOf)
                .distinct()
                .sorted()
                .limit(MAX_DISTINCT_VALUES)
                .toList();
    }

    private String geometryTypeName(GeometryKind kind) {
        return switch (kind) {
            case POINT -> "Point";
            case LINE -> "LineString";
            case POLYGON -> "Polygon";
        };
    }
}
