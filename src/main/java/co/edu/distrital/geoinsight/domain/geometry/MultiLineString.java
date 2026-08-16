package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/** Conjunto de líneas (variante multiparte real de los datasets). */
public final class MultiLineString extends Geometry {

    private final List<LineString> lineStrings;

    public MultiLineString(List<LineString> lineStrings) {
        if (lineStrings == null || lineStrings.isEmpty()) {
            throw new IllegalArgumentException("MultiLineString requiere al menos una línea");
        }
        this.lineStrings = List.copyOf(lineStrings);
    }

    public List<LineString> lineStrings() {
        return lineStrings;
    }

    @Override
    public String geoJsonType() {
        return "MultiLineString";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        double min = Double.POSITIVE_INFINITY;
        for (LineString line : lineStrings) {
            min = Math.min(min, line.distanceMeters(point));
        }
        return min;
    }

    @Override
    public boolean contains(Coordinate point) {
        return false;
    }

    @Override
    public Bounds bounds() {
        return lineStrings.stream()
                .map(LineString::bounds)
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
