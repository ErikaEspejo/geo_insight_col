package co.edu.distrital.geoinsight.application.exploration;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import co.edu.distrital.geoinsight.domain.repository.GeoEntityRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LayerExplorationServiceTest {

    private final FakeGeoEntityRepository geoEntities = new FakeGeoEntityRepository();
    private final FakeDatasetRepository datasets = new FakeDatasetRepository();
    private final LayerExplorationService service = new LayerExplorationService(
            new EntityCatalog(datasets, geoEntities), datasets);

    @Test
    void layerIsAvailableWhenDatasetPresentWithData() {
        geoEntities.save(entity(Domain.VOLCAN));

        LayerMetadata volcan = layer(Domain.VOLCAN);

        assertThat(volcan.dataAvailable()).isTrue();
    }

    @Test
    void layerIsUnavailableWhenDatasetMissing() {
        datasets.missing.add(Domain.VOLCAN);
        geoEntities.save(entity(Domain.VOLCAN));

        LayerMetadata volcan = layer(Domain.VOLCAN);

        assertThat(volcan.dataAvailable()).isFalse();
    }

    @Test
    void layerIsUnavailableWhenNothingLoaded() {
        LayerMetadata volcan = layer(Domain.VOLCAN);

        assertThat(volcan.dataAvailable()).isFalse();
    }

    @Test
    void valuesOfSameAttributeUseOrAndDifferentAttributesUseAnd() {
        datasets.attributes.addAll(Set.of("TIPO", "ESTADO"));
        geoEntities.save(entity("v1", Map.of("TIPO", "A", "ESTADO", "ACTIVO")));
        geoEntities.save(entity("v2", Map.of("TIPO", "B", "ESTADO", "ACTIVO")));
        geoEntities.save(entity("v3", Map.of("TIPO", "A", "ESTADO", "INACTIVO")));
        geoEntities.save(entity("v4", Map.of("TIPO", "C", "ESTADO", "ACTIVO")));

        List<GeoscienceEntity> result = service.entitiesWithFilters(Domain.VOLCAN,
                Map.of("TIPO", List.of("A", "B"), "ESTADO", List.of("ACTIVO")));

        assertThat(result).extracting(GeoscienceEntity::id).containsExactlyInAnyOrder("v1", "v2");
    }

    private LayerMetadata layer(Domain domain) {
        return service.layers().stream()
                .filter(l -> l.domain() == domain)
                .findFirst().orElseThrow();
    }

    private static GeoscienceEntity entity(Domain domain) {
        return new GeoscienceEntity("test-1", domain, Origin.GEOINSIGHT,
                GeometryFactory.point(new Coordinate(-74.07, 4.71)), Map.of());
    }

    private static GeoscienceEntity entity(String id, Map<String, Object> attributes) {
        return new GeoscienceEntity(id, Domain.VOLCAN, Origin.GEOINSIGHT,
                GeometryFactory.point(new Coordinate(-74.07, 4.71)), attributes);
    }

    private static class FakeGeoEntityRepository implements GeoEntityRepository {
        private final Map<String, GeoscienceEntity> entities = new LinkedHashMap<>();

        @Override
        public List<GeoscienceEntity> findAll() {
            return new ArrayList<>(entities.values());
        }

        @Override
        public List<GeoscienceEntity> findByDomain(Domain domain) {
            return entities.values().stream().filter(e -> e.domain() == domain).toList();
        }

        @Override
        public Optional<GeoscienceEntity> findById(String id) {
            return Optional.ofNullable(entities.get(id));
        }

        @Override
        public GeoscienceEntity save(GeoscienceEntity entity) {
            entities.put(entity.id(), entity);
            return entity;
        }

        @Override
        public void delete(String id) {
            entities.remove(id);
        }
    }

    private static class FakeDatasetRepository implements DatasetRepository {
        private final Set<Domain> missing = new LinkedHashSet<>();
        private final Set<String> attributes = new LinkedHashSet<>();

        @Override
        public List<GeoscienceEntity> findSgcByDomain(Domain domain) {
            return List.of();
        }

        @Override
        public Set<String> attributeNames(Domain domain) {
            return Set.copyOf(attributes);
        }

        @Override
        public Set<String> requiredAttributes(Domain domain) {
            return Set.of();
        }

        @Override
        public Set<String> editableAttributes(Domain domain) {
            return Set.of();
        }

        @Override
        public Map<String, AttributeValueType> editableAttributeTypes(Domain domain) {
            return Map.of();
        }

        @Override
        public List<Object> distinctValues(Domain domain, String attribute) {
            return List.of();
        }

        @Override
        public boolean acceptsGeometry(Domain domain, Geometry geometry) {
            return domain.geometryKind().accepts(geometry);
        }

        @Override
        public boolean isDatasetLoaded(Domain domain) {
            return !missing.contains(domain);
        }

        @Override
        public Set<Domain> missingDatasets() {
            return Set.copyOf(missing);
        }

        @Override
        public Optional<GeoscienceEntity> findSgcById(String id) {
            return Optional.empty();
        }
    }
}
