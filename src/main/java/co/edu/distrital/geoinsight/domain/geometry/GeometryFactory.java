package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/**
 * Fábrica de geometrías con validación. El dominio no conoce GeoJSON; el
 * parsing del formato vive en infraestructura, que usa esta fábrica.
 */
public final class GeometryFactory {

    private GeometryFactory() {
    }

    public static Point point(Coordinate coordinate) {
        return new Point(coordinate);
    }

    public static LineString lineString(List<Coordinate> coordinates) {
        return new LineString(coordinates);
    }

    public static Polygon polygon(List<Coordinate> exteriorRing, List<List<Coordinate>> holes) {
        return new Polygon(exteriorRing, holes);
    }

    public static MultiPoint multiPoint(List<Point> points) {
        return new MultiPoint(points);
    }

    public static MultiLineString multiLineString(List<LineString> lineStrings) {
        return new MultiLineString(lineStrings);
    }

    public static MultiPolygon multiPolygon(List<Polygon> polygons) {
        return new MultiPolygon(polygons);
    }
}
