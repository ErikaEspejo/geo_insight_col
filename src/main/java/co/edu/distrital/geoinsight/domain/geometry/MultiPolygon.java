package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/** Conjunto de polígonos (variante multiparte real de los datasets). */
public final class MultiPolygon extends Geometry {

    private final List<Polygon> polygons;

    public MultiPolygon(List<Polygon> polygons) {
        if (polygons == null || polygons.isEmpty()) {
            throw new IllegalArgumentException("MultiPolygon requiere al menos un polígono");
        }
        this.polygons = List.copyOf(polygons);
    }

    public List<Polygon> polygons() {
        return polygons;
    }

    @Override
    public String geoJsonType() {
        return "MultiPolygon";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        double min = Double.POSITIVE_INFINITY;
        for (Polygon polygon : polygons) {
            min = Math.min(min, polygon.distanceMeters(point));
        }
        return min;
    }

    @Override
    public boolean contains(Coordinate point) {
        return polygons.stream().anyMatch(p -> p.contains(point));
    }

    @Override
    public Bounds bounds() {
        return polygons.stream()
                .map(Polygon::bounds)
                .reduce(null, this::union);
    }

    private Bounds union(Bounds a, Bounds b) {
        if (a == null) {
            return b;
        }
        return new Bounds(
                Math.min(a.minLon(), b.minLon()),
                Math.min(a.minLat(), b.minLat()),
                Math.max(a.maxLon(), b.maxLon()),
                Math.max(a.maxLat(), b.maxLat()));
    }
}
