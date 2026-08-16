package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/** Conjunto de puntos (variante multiparte real de los datasets). */
public final class MultiPoint extends Geometry {

    private final List<Point> points;

    public MultiPoint(List<Point> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("MultiPoint requiere al menos un punto");
        }
        this.points = List.copyOf(points);
    }

    public List<Point> points() {
        return points;
    }

    @Override
    public String geoJsonType() {
        return "MultiPoint";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        double min = Double.POSITIVE_INFINITY;
        for (Point p : points) {
            min = Math.min(min, p.distanceMeters(point));
        }
        return min;
    }

    @Override
    public boolean contains(Coordinate point) {
        return points.stream().anyMatch(p -> p.contains(point));
    }

    @Override
    public Bounds bounds() {
        double minLon = Double.POSITIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        for (Point p : points) {
            Coordinate c = p.coordinate();
            minLon = Math.min(minLon, c.lon());
            minLat = Math.min(minLat, c.lat());
            maxLon = Math.max(maxLon, c.lon());
            maxLat = Math.max(maxLat, c.lat());
        }
        return new Bounds(minLon, minLat, maxLon, maxLat);
    }
}
