package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Repositorio de solo lectura de los cinco datasets SGC (FR-003). Carga los
 * GeoJSON locales en memoria y expone entidades, atributos reales y valores
 * distintos (FR-008, FR-018).
 */
@Repository
public class GeoJsonDatasetRepository implements DatasetRepository {

    private final Path datasetsDir;
    private final ObjectMapper objectMapper;

    private final Map<Domain, List<GeoscienceEntity>> entitiesByDomain = new EnumMap<>(Domain.class);
    private final Map<Domain, Set<String>> presentAttributes = new EnumMap<>(Domain.class);
    private final Map<Domain, Map<String, List<Object>>> distinctValues = new EnumMap<>(Domain.class);
    private final Map<Domain, Map<String, AttributeValueType>> attributeTypes = new EnumMap<>(Domain.class);
    private final Set<Domain> missing = new LinkedHashSet<>();

    public GeoJsonDatasetRepository(Path datasetsDir, ObjectMapper objectMapper) {
        this.datasetsDir = datasetsDir;
        this.objectMapper = objectMapper;
    }

    /**
     * Carga eager de los cinco datasets al construir el bean, antes de que el
     * servidor empiece a atender peticiones. Evita servir capas parcialmente
     * cargadas durante el arranque (los ApplicationRunner corren después de que
     * Tomcat ya escucha).
     */
    @PostConstruct
    public void loadAll() {
        entitiesByDomain.clear();
        presentAttributes.clear();
        distinctValues.clear();
        attributeTypes.clear();
        missing.clear();

        for (Domain domain : Domain.values()) {
            loadDomain(domain);
        }
    }

    private void loadDomain(Domain domain) {
        SgcDatasets.Source source = SgcDatasets.BY_DOMAIN.get(domain);
        Path file = datasetsDir.resolve(source.fileName());
        if (!Files.exists(file)) {
            missing.add(domain);
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            JsonNode features = root.path("features");
            List<GeoscienceEntity> entities = new ArrayList<>();
            Set<String> attributeNames = new LinkedHashSet<>();
            int index = 0;
            for (JsonNode feature : features) {
                JsonNode geometryNode = feature.get("geometry");
                if (geometryNode == null || geometryNode.isNull()) {
                    continue;
                }
                Geometry geometry;
                try {
                    geometry = GeoJsonGeometryParser.parse(geometryNode);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                Map<String, Object> attributes = parseAttributes(feature.get("properties"));
                attributeNames.addAll(attributes.keySet());
                Object objectId = attributes.get("OBJECTID");
                String id = "SGC-" + domain.name() + "-" + (objectId == null ? index : objectId);
                entities.add(new GeoscienceEntity(id, domain, Origin.SGC, geometry, attributes));
                index++;
            }
            entitiesByDomain.put(domain, List.copyOf(entities));
            presentAttributes.put(domain, Collections.unmodifiableSet(attributeNames));
            distinctValues.put(domain, computeDistinctValues(entities));
            attributeTypes.put(domain, computeAttributeTypes(entities));
            if (entities.isEmpty()) {
                missing.add(domain);
            }
        } catch (IOException e) {
            missing.add(domain);
        }
    }

    private Map<String, Object> parseAttributes(JsonNode properties) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (properties == null || properties.isNull() || !properties.isObject()) {
            return attributes;
        }
        properties.fields().forEachRemaining(entry -> attributes.put(entry.getKey(), scalarValue(entry.getValue())));
        return attributes;
    }

    private static Object scalarValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isFloatingPointNumber()) {
            return node.doubleValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.asText();
    }

    private Map<String, List<Object>> computeDistinctValues(List<GeoscienceEntity> entities) {
        Map<String, Set<Object>> valuesByAttribute = new LinkedHashMap<>();
        for (GeoscienceEntity entity : entities) {
            for (Map.Entry<String, Object> entry : entity.attributes().entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                valuesByAttribute.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>()).add(entry.getValue());
            }
        }
        Map<String, List<Object>> result = new LinkedHashMap<>();
        valuesByAttribute.forEach((attribute, values) -> result.put(attribute, List.copyOf(values)));
        return result;
    }

    private Map<String, AttributeValueType> computeAttributeTypes(List<GeoscienceEntity> entities) {
        Map<String, AttributeValueType> result = new LinkedHashMap<>();
        for (GeoscienceEntity entity : entities) {
            entity.attributes().forEach((attribute, value) -> {
                if (value == null) return;
                AttributeValueType observed = valueType(value);
                result.merge(attribute, observed, (existing, next) -> existing == next ? existing : AttributeValueType.TEXT);
            });
        }
        return Collections.unmodifiableMap(result);
    }

    private AttributeValueType valueType(Object value) {
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return AttributeValueType.INTEGER;
        }
        if (value instanceof Number) return AttributeValueType.DECIMAL;
        if (value instanceof Boolean) return AttributeValueType.BOOLEAN;
        return AttributeValueType.TEXT;
    }

    public Map<Domain, Integer> counts() {
        Map<Domain, Integer> counts = new EnumMap<>(Domain.class);
        entitiesByDomain.forEach((domain, entities) -> counts.put(domain, entities.size()));
        return counts;
    }

    public Map<Domain, Set<String>> presentAttributes() {
        return presentAttributes;
    }

    @Override
    public List<GeoscienceEntity> findSgcByDomain(Domain domain) {
        return entitiesByDomain.getOrDefault(domain, List.of());
    }

    @Override
    public Set<String> attributeNames(Domain domain) {
        List<String> candidates = DomainCatalogs.FILTER_ATTRIBUTES.get(domain);
        Set<String> present = presentAttributes.getOrDefault(domain, Set.of());
        Set<String> result = new LinkedHashSet<>();
        for (String candidate : candidates) {
            if (present.contains(candidate)) {
                result.add(candidate);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public List<Object> distinctValues(Domain domain, String attribute) {
        return distinctValues.getOrDefault(domain, Map.of()).getOrDefault(attribute, List.of());
    }

    @Override
    public Set<String> requiredAttributes(Domain domain) {
        return Set.copyOf(DomainCatalogs.REQUIRED_FIELDS.getOrDefault(domain, List.of()));
    }

    @Override
    public Set<String> editableAttributes(Domain domain) {
        Set<String> present = presentAttributes.getOrDefault(domain, Set.of());
        Map<String, AttributeValueType> typed = attributeTypes.getOrDefault(domain, Map.of());
        Set<String> editable = new LinkedHashSet<>();
        for (String candidate : DomainCatalogs.EDITABLE_ATTRIBUTES.getOrDefault(domain, List.of())) {
            if (present.contains(candidate) && typed.containsKey(candidate)) editable.add(candidate);
        }
        return Collections.unmodifiableSet(editable);
    }

    @Override
    public Map<String, AttributeValueType> editableAttributeTypes(Domain domain) {
        Map<String, AttributeValueType> types = attributeTypes.getOrDefault(domain, Map.of());
        Map<String, AttributeValueType> editable = new LinkedHashMap<>();
        for (String attribute : editableAttributes(domain)) editable.put(attribute, types.get(attribute));
        return Collections.unmodifiableMap(editable);
    }

    @Override
    public boolean acceptsGeometry(Domain domain, Geometry geometry) {
        return domain.geometryKind().accepts(geometry);
    }

    @Override
    public boolean isDatasetLoaded(Domain domain) {
        return !missing.contains(domain) && entitiesByDomain.containsKey(domain);
    }

    @Override
    public Set<Domain> missingDatasets() {
        return Set.copyOf(missing);
    }

    @Override
    public Optional<GeoscienceEntity> findSgcById(String id) {
        return entitiesByDomain.values().stream()
                .flatMap(List::stream)
                .filter(entity -> entity.id().equals(id))
                .findFirst();
    }
}
