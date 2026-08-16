package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/** Secuencia de puntos (línea). La distancia es la mínima a cualquiera de sus segmentos. */
public final class LineString extends Geometry {

    private final List<Coordinate> coordinates;

    public LineString(List<Coordinate> coordinates) {
        this.coordinates = requireAtLeast(coordinates, 2, "LineString");
    }

    public List<Coordinate> coordinates() {
        return coordinates;
    }

    @Override
    public String geoJsonType() {
        return "LineString";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < coordinates.size() - 1; i++) {
            min = Math.min(min, GeoMath.distancePointToSegmentMeters(point, coordinates.get(i), coordinates.get(i + 1)));
        }
        return min;
    }

    @Override
    public boolean contains(Coordinate point) {
        return false;
    }

    @Override
    public Bounds bounds() {
        return boundsOf(coordinates);
    }

    static Bounds boundsOf(List<Coordinate> coordinates) {
        double minLon = Double.POSITIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        for (Coordinate c : coordinates) {
            minLon = Math.min(minLon, c.lon());
            minLat = Math.min(minLat, c.lat());
            maxLon = Math.max(maxLon, c.lon());
            maxLat = Math.max(maxLat, c.lat());
        }
        return new Bounds(minLon, minLat, maxLon, maxLat);
    }
}
