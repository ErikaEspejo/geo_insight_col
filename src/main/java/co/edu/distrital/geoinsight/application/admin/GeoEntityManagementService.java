package co.edu.distrital.geoinsight.application.admin;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.application.common.EntityNotFoundException;
import co.edu.distrital.geoinsight.application.common.ForbiddenOperationException;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import co.edu.distrital.geoinsight.domain.repository.GeoEntityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * US6 — Gestión de entidades GEOINSIGHT (FR-004, FR-005). Los registros SGC
 * son de solo lectura (FR-003); los campos obligatorios por dominio derivan de
 * los datasets (FR-018); la geometría debe ser admitida por el dominio
 * (FR-017).
 */
@Service
public class GeoEntityManagementService {

    private final EntityCatalog catalog;
    private final GeoEntityRepository geoEntityRepository;
    private final DatasetRepository datasetRepository;

    public GeoEntityManagementService(EntityCatalog catalog, GeoEntityRepository geoEntityRepository,
                                      DatasetRepository datasetRepository) {
        this.catalog = catalog;
        this.geoEntityRepository = geoEntityRepository;
        this.datasetRepository = datasetRepository;
    }

    public GeoscienceEntity create(Domain domain, Geometry geometry, Map<String, Object> attributes) {
        validate(domain, geometry, attributes);
        GeoscienceEntity entity = new GeoscienceEntity(
                "GEO-" + UUID.randomUUID(), domain, Origin.GEOINSIGHT, geometry, attributes);
        return geoEntityRepository.save(entity);
    }

    public GeoscienceEntity update(String id, Domain domain, Geometry geometry, Map<String, Object> attributes) {
        GeoscienceEntity existing = requireEditable(id);
        if (existing.domain() != domain) {
            throw new IllegalArgumentException("El dominio de la solicitud no coincide con la entidad existente");
        }
        validate(domain, geometry, attributes);
        return geoEntityRepository.save(new GeoscienceEntity(
                existing.id(), existing.domain(), Origin.GEOINSIGHT, geometry, attributes));
    }

    public void delete(String id) {
        requireEditable(id);
        geoEntityRepository.delete(id);
    }

    public List<GeoscienceEntity> findAll() {
        return geoEntityRepository.findAll();
    }

    private GeoscienceEntity requireEditable(String id) {
        Optional<GeoscienceEntity> sgc = catalog.findById(id)
                .filter(entity -> entity.origin() == Origin.SGC);
        if (sgc.isPresent()) {
            throw new ForbiddenOperationException("Los registros SGC son de solo lectura");
        }
        return geoEntityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entidad no encontrada: " + id));
    }

    private void validate(Domain domain, Geometry geometry, Map<String, Object> attributes) {
        if (domain == null || geometry == null) {
            throw new IllegalArgumentException("Dominio y geometría son obligatorios");
        }
        if (!domain.geometryKind().accepts(geometry)) {
            throw new IllegalArgumentException("La geometría no es admitida para el dominio " + domain.name());
        }
        var unexpected = attributes.keySet().stream()
                .filter(attribute -> !datasetRepository.editableAttributes(domain).contains(attribute))
                .sorted()
                .toList();
        if (!unexpected.isEmpty()) {
            throw new IllegalArgumentException("Atributos no admitidos para " + domain.name() + ": "
                    + String.join(", ", unexpected));
        }
        Map<String, AttributeValueType> types = datasetRepository.editableAttributeTypes(domain);
        attributes.forEach((attribute, value) -> validateAttributeType(attribute, value, types.get(attribute)));
        for (String required : datasetRepository.requiredAttributes(domain)) {
            Object value = attributes.get(required);
            if (value == null || (value instanceof String text && text.isBlank())) {
                throw new IllegalArgumentException("El campo obligatorio no está presente: " + required);
            }
        }
    }

    private void validateAttributeType(String attribute, Object value, AttributeValueType expected) {
        if (value == null || expected == null) return;
        boolean valid = switch (expected) {
            case TEXT -> value instanceof String;
            case INTEGER -> value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long;
            case DECIMAL -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
        };
        if (!valid) {
            throw new IllegalArgumentException("El campo " + attribute + " debe ser de tipo " + expected.name());
        }
    }
}
