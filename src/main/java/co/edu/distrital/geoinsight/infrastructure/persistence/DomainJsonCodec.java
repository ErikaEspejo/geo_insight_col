package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.LineString;
import co.edu.distrital.geoinsight.domain.geometry.Point;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Codificación de entidades de dominio a/desde JSON. Reside en infraestructura
 * para mantener el dominio independiente de Jackson.
 */
public final class DomainJsonCodec {

    private DomainJsonCodec() {
    }

    public static ObjectNode entityToJson(ObjectMapper objectMapper, GeoscienceEntity entity) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", entity.id());
        node.put("domain", entity.domain().name());
        node.put("origin", entity.origin().name());
        node.set("geometry", geometryToJson(objectMapper, entity.geometry()));
        ObjectNode attributes = node.putObject("attributes");
        entity.attributes().forEach((key, value) -> attributes.set(key, objectMapper.valueToTree(value)));
        return node;
    }

    public static GeoscienceEntity entityFromJson(ObjectMapper objectMapper, JsonNode node) {
        String id = node.path("id").asText();
        Domain domain = Domain.fromKey(node.path("domain").asText())
                .orElseThrow(() -> new IllegalArgumentException("Dominio inválido: " + node.path("domain")));
        Origin origin = Origin.valueOf(node.path("origin").asText());
        Geometry geometry = geometryFromJson(node.path("geometry"));
        Map<String, Object> attributes = new LinkedHashMap<>();
        JsonNode attributesNode = node.path("attributes");
        if (attributesNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = attributesNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                attributes.put(entry.getKey(), scalarValue(entry.getValue()));
            }
        }
        return new GeoscienceEntity(id, domain, origin, geometry, attributes);
    }

    public static ObjectNode geometryToJson(ObjectMapper objectMapper, Geometry geometry) {
        ObjectNode node = objectMapper.createObjectNode();
        if (geometry instanceof Point point) {
            node.put("type", "Point");
            node.set("coordinates", coordinateArray(objectMapper, point.coordinate()));
        } else if (geometry instanceof LineString line) {
            node.put("type", "LineString");
            node.set("coordinates", coordinateArray(objectMapper, line.coordinates()));
        } else if (geometry instanceof Polygon polygon) {
            node.put("type", "Polygon");
            node.set("coordinates", ringsArray(objectMapper, polygon));
        } else if (geometry instanceof co.edu.distrital.geoinsight.domain.geometry.MultiPoint multiPoint) {
            node.put("type", "MultiPoint");
            ArrayNode array = node.putArray("coordinates");
            for (Point p : multiPoint.points()) {
                array.add(coordinateArray(objectMapper, p.coordinate()));
            }
        } else if (geometry instanceof co.edu.distrital.geoinsight.domain.geometry.MultiLineString multiLine) {
            node.put("type", "MultiLineString");
            ArrayNode array = node.putArray("coordinates");
            for (LineString line : multiLine.lineStrings()) {
                array.add(coordinateArray(objectMapper, line.coordinates()));
            }
        } else if (geometry instanceof co.edu.distrital.geoinsight.domain.geometry.MultiPolygon multiPolygon) {
            node.put("type", "MultiPolygon");
            ArrayNode array = node.putArray("coordinates");
            for (Polygon p : multiPolygon.polygons()) {
                array.add(ringsArray(objectMapper, p));
            }
        } else {
            throw new IllegalArgumentException("Geometría no soportada: " + geometry.getClass());
        }
        return node;
    }

    public static Geometry geometryFromJson(JsonNode node) {
        return GeoJsonGeometryParser.parse(node);
    }

    private static ArrayNode coordinateArray(ObjectMapper objectMapper, List<Coordinate> coordinates) {
        ArrayNode array = objectMapper.createArrayNode();
        for (Coordinate c : coordinates) {
            array.add(coordinateArray(objectMapper, c));
        }
        return array;
    }

    private static ArrayNode coordinateArray(ObjectMapper objectMapper, Coordinate c) {
        ArrayNode array = objectMapper.createArrayNode();
        array.add(c.lon());
        array.add(c.lat());
        return array;
    }

    private static ArrayNode ringsArray(ObjectMapper objectMapper, Polygon polygon) {
        ArrayNode rings = objectMapper.createArrayNode();
        rings.add(coordinateArray(objectMapper, polygon.exteriorRing()));
        for (List<Coordinate> hole : polygon.holes()) {
            rings.add(coordinateArray(objectMapper, hole));
        }
        return rings;
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
}
