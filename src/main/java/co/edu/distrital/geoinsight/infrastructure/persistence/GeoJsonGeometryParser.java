package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.LineString;
import co.edu.distrital.geoinsight.domain.geometry.Point;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Traducción de geometría GeoJSON a geometría de dominio. Vive en
 * infraestructura para mantener el dominio independiente de Jackson.
 */
final class GeoJsonGeometryParser {

    private GeoJsonGeometryParser() {
    }

    static Geometry parse(JsonNode geometryNode) {
        if (geometryNode == null || geometryNode.isNull() || !geometryNode.has("type")) {
            throw new IllegalArgumentException("Geometría GeoJSON inválida");
        }
        String type = geometryNode.get("type").asText();
        return switch (type) {
            case "Point" -> point(geometryNode);
            case "MultiPoint" -> multiPoint(geometryNode);
            case "LineString" -> lineString(geometryNode);
            case "MultiLineString" -> multiLineString(geometryNode);
            case "Polygon" -> polygon(geometryNode);
            case "MultiPolygon" -> multiPolygon(geometryNode);
            default -> throw new IllegalArgumentException("Tipo de geometría no soportado: " + type);
        };
    }

    private static Geometry point(JsonNode node) {
        return GeometryFactory.point(coordinate(node.get("coordinates")));
    }

    private static Geometry multiPoint(JsonNode node) {
        List<Point> points = new ArrayList<>();
        for (JsonNode c : node.get("coordinates")) {
            points.add(GeometryFactory.point(coordinate(c)));
        }
        return GeometryFactory.multiPoint(points);
    }

    private static Geometry lineString(JsonNode node) {
        return GeometryFactory.lineString(coordinates(node.get("coordinates")));
    }

    private static Geometry multiLineString(JsonNode node) {
        List<LineString> lines = new ArrayList<>();
        for (JsonNode line : node.get("coordinates")) {
            lines.add(GeometryFactory.lineString(coordinates(line)));
        }
        return GeometryFactory.multiLineString(lines);
    }

    private static Geometry polygon(JsonNode node) {
        return polygonParts(node.get("coordinates"));
    }

    private static Geometry multiPolygon(JsonNode node) {
        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode polygonCoordinates : node.get("coordinates")) {
            polygons.add((Polygon) polygonParts(polygonCoordinates));
        }
        return GeometryFactory.multiPolygon(polygons);
    }

    private static Geometry polygonParts(JsonNode ringsNode) {
        JsonNode exterior = ringsNode.get(0);
        List<List<Coordinate>> holes = new ArrayList<>();
        for (int i = 1; i < ringsNode.size(); i++) {
            holes.add(coordinates(ringsNode.get(i)));
        }
        return GeometryFactory.polygon(coordinates(exterior), holes);
    }

    private static List<Coordinate> coordinates(JsonNode coordinatesNode) {
        List<Coordinate> result = new ArrayList<>(coordinatesNode.size());
        for (JsonNode c : coordinatesNode) {
            result.add(coordinate(c));
        }
        return result;
    }

    private static Coordinate coordinate(JsonNode coordinateNode) {
        return new Coordinate(coordinateNode.get(0).asDouble(), coordinateNode.get(1).asDouble());
    }
}
