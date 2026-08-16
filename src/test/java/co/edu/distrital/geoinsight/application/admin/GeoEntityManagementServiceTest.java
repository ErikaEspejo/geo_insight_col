package co.edu.distrital.geoinsight.application.admin;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.application.common.EntityNotFoundException;
import co.edu.distrital.geoinsight.application.common.ForbiddenOperationException;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.Point;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import co.edu.distrital.geoinsight.domain.repository.GeoEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoEntityManagementServiceTest {

    private GeoEntityManagementService service;
    private FakeGeoEntityRepository geoRepository;
    private FakeDatasetRepository datasetRepository;

    private static final Point BOGOTA = GeometryFactory.point(new Coordinate(-74.07, 4.71));

    @BeforeEach
    void setUp() {
        geoRepository = new FakeGeoEntityRepository();
        datasetRepository = new FakeDatasetRepository();
        service = new GeoEntityManagementService(
                new EntityCatalog(datasetRepository, geoRepository), geoRepository, datasetRepository);
    }

    @Test
    void createPersistsGeoInsightEntityWithGeneratedId() {
        GeoscienceEntity entity = service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "Nevado Test"));

        assertThat(entity.id()).startsWith("GEO-");
        assertThat(entity.origin()).isEqualTo(Origin.GEOINSIGHT);
        assertThat(entity.domain()).isEqualTo(Domain.VOLCAN);
        assertThat(geoRepository.findById(entity.id())).contains(entity);
    }

    @Test
    void createRejectsMissingRequiredAttribute() {
        assertThatThrownBy(() -> service.create(Domain.VOLCAN, BOGOTA, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NombreVolcan");
    }

    @Test
    void createRejectsBlankRequiredAttribute() {
        assertThatThrownBy(() -> service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsAttributesOutsideDatasetWhitelist() {
        assertThatThrownBy(() -> service.create(Domain.VOLCAN, BOGOTA,
                Map.of("NombreVolcan", "Permitido", "CampoInventado", "No")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CampoInventado");
    }

    @Test
    void createRejectsNumericAttributeSentAsText() {
        assertThatThrownBy(() -> service.create(Domain.MOVIMIENTO_EN_MASA, BOGOTA,
                Map.of("TIPO", "Flujo", "ID", "123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID").hasMessageContaining("INTEGER");
    }

    @Test
    void createRejectsGeometryNotAcceptedByDomain() {
        List<Coordinate> ring = List.of(
                new Coordinate(-74.0, 4.0), new Coordinate(-74.1, 4.0), new Coordinate(-74.1, 4.1),
                new Coordinate(-74.0, 4.1), new Coordinate(-74.0, 4.0));
        assertThatThrownBy(() -> service.create(Domain.VOLCAN,
                GeometryFactory.polygon(ring, List.of()), Map.of("NombreVolcan", "X")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("geometría");
    }

    @Test
    void updatePersistsChanges() {
        GeoscienceEntity created = service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "Original"));

        GeoscienceEntity updated = service.update(created.id(), Domain.VOLCAN,
                GeometryFactory.point(new Coordinate(-74.0, 4.8)), Map.of("NombreVolcan", "Renombrado"));

        assertThat(updated.origin()).isEqualTo(Origin.GEOINSIGHT);
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.attributes()).containsEntry("NombreVolcan", "Renombrado");
        assertThat(geoRepository.findById(created.id())).contains(updated);
    }

    @Test
    void updateRejectsDomainMismatch() {
        GeoscienceEntity created = service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "Original"));

        assertThatThrownBy(() -> service.update(created.id(), Domain.MOVIMIENTO_EN_MASA,
                BOGOTA, Map.of("TIPO", "Deslizamiento")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dominio");
    }

    @Test
    void deleteRemovesEntity() {
        GeoscienceEntity created = service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "A"));

        service.delete(created.id());

        assertThat(geoRepository.findById(created.id())).isEmpty();
    }

    @Test
    void sgcEntitiesCannotBeUpdatedOrDeleted() {
        datasetRepository.sgc = new GeoscienceEntity("SGC-VOLCAN-1", Domain.VOLCAN, Origin.SGC,
                BOGOTA, Map.of("NombreVolcan", "SGC"));

        assertThatThrownBy(() -> service.update("SGC-VOLCAN-1", Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "X")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.delete("SGC-VOLCAN-1"))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void unknownEntityCausesNotFoundException() {
        assertThatThrownBy(() -> service.delete("GEO-inexistente"))
                .isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.update("GEO-inexistente", Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "X")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findAllListsOnlyGeoInsightEntities() {
        datasetRepository.sgc = new GeoscienceEntity("SGC-VOLCAN-1", Domain.VOLCAN, Origin.SGC,
                BOGOTA, Map.of("NombreVolcan", "SGC"));
        service.create(Domain.VOLCAN, BOGOTA, Map.of("NombreVolcan", "A"));

        assertThat(service.findAll()).extracting(GeoscienceEntity::origin)
                .containsOnly(Origin.GEOINSIGHT);
    }

    static class FakeGeoEntityRepository implements GeoEntityRepository {
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

    static class FakeDatasetRepository implements DatasetRepository {
        GeoscienceEntity sgc;

        @Override
        public List<GeoscienceEntity> findSgcByDomain(Domain domain) {
            return sgc != null && sgc.domain() == domain ? List.of(sgc) : List.of();
        }

        @Override
        public Set<String> attributeNames(Domain domain) {
            return Set.of();
        }

        @Override
        public Set<String> requiredAttributes(Domain domain) {
            return switch (domain) {
                case VOLCAN -> Set.of("NombreVolcan");
                case MOVIMIENTO_EN_MASA -> Set.of("TIPO");
                case FALLA_GEOLOGICA -> Set.of("NombreFalla");
                case UNIDAD_GEOLOGICA -> Set.of("SimboloUC");
                case DOMINIO_TECTONICO -> Set.of("NombreDT");
            };
        }

        @Override
        public Set<String> editableAttributes(Domain domain) {
            if (domain == Domain.MOVIMIENTO_EN_MASA) return Set.of("TIPO", "ID");
            return requiredAttributes(domain);
        }

        @Override
        public Map<String, AttributeValueType> editableAttributeTypes(Domain domain) {
            Map<String, AttributeValueType> types = new LinkedHashMap<>();
            editableAttributes(domain).forEach(attribute -> types.put(attribute,
                    attribute.equals("ID") ? AttributeValueType.INTEGER : AttributeValueType.TEXT));
            return types;
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
            return sgc != null;
        }

        @Override
        public Set<Domain> missingDatasets() {
            return Set.of();
        }

        @Override
        public Optional<GeoscienceEntity> findSgcById(String id) {
            return sgc != null && sgc.id().equals(id) ? Optional.of(sgc) : Optional.empty();
        }
    }
}
