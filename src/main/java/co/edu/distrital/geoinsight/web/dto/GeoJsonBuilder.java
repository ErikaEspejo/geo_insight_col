package co.edu.distrital.geoinsight.web.dto;

import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.infrastructure.persistence.DomainJsonCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.function.Function;

/**
 * Construcción de GeoJSON (FeatureCollection) para la visualización con
 * Leaflet. Transformación de presentación: el análisis espacial permanece en
 * el dominio. Admite una transformación de geometría SOLO visual (simplificada);
 * el análisis nunca usa estas salidas.
 */
public final class GeoJsonBuilder {

    private GeoJsonBuilder() {
    }

    public static ObjectNode entitiesToFeatureCollection(ObjectMapper objectMapper, List<GeoscienceEntity> entities) {
        return entitiesToFeatureCollection(objectMapper, entities, Function.identity());
    }

    public static ObjectNode entitiesToFeatureCollection(ObjectMapper objectMapper, List<GeoscienceEntity> entities,
                                                         Function<Geometry, Geometry> geometryTransform) {
        ObjectNode collection = objectMapper.createObjectNode();
        collection.put("type", "FeatureCollection");
        ArrayNode features = collection.putArray("features");
        for (GeoscienceEntity entity : entities) {
            features.add(entityToFeature(objectMapper, entity, geometryTransform));
        }
        return collection;
    }

    public static ObjectNode entityToFeature(ObjectMapper objectMapper, GeoscienceEntity entity) {
        return entityToFeature(objectMapper, entity, Function.identity());
    }

    public static ObjectNode entityToFeature(ObjectMapper objectMapper, GeoscienceEntity entity,
                                             Function<Geometry, Geometry> geometryTransform) {
        ObjectNode feature = objectMapper.createObjectNode();
        feature.put("type", "Feature");
        feature.put("id", entity.id());
        feature.put("domain", entity.domain().name());
        feature.put("origin", entity.origin().name());
        feature.set("geometry", DomainJsonCodec.geometryToJson(objectMapper, geometryTransform.apply(entity.geometry())));
        ObjectNode properties = feature.putObject("properties");
        properties.put("origin", entity.origin().name());
        entity.attributes().forEach((key, value) -> properties.set(key, objectMapper.valueToTree(value)));
        return feature;
    }
}
