package co.edu.distrital.geoinsight.application.exploration;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.MultiPolygon;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplificación de geometría SOLO para visualización (contingencia SC-001,
 * research.md §3): reduce los anillos de polígonos con Douglas-Peucker y
 * redondea coordenadas. El análisis y las consultas usan siempre la geometría
 * completa; esta simplificación nunca cruza a esos flujos.
 */
public final class GeometrySimplifier {

    private final double toleranceDegrees;
    private final int decimalPlaces;

    public GeometrySimplifier(double toleranceDegrees, int decimalPlaces) {
        if (toleranceDegrees <= 0.0) {
            throw new IllegalArgumentException("La tolerancia debe ser positiva");
        }
        if (decimalPlaces < 0 || decimalPlaces > 8) {
            throw new IllegalArgumentException("Precisión de coordenadas fuera de rango");
        }
        this.toleranceDegrees = toleranceDegrees;
        this.decimalPlaces = decimalPlaces;
    }

    public Geometry simplify(Geometry geometry) {
        if (geometry instanceof Polygon polygon) {
            return new Polygon(simplifyRing(polygon.exteriorRing()),
                    polygon.holes().stream().map(this::simplifyRing).toList());
        }
        if (geometry instanceof MultiPolygon multiPolygon) {
            return new MultiPolygon(multiPolygon.polygons().stream()
                    .map(polygon -> (Polygon) simplify(polygon))
                    .toList());
        }
        return geometry;
    }

    private List<Coordinate> simplifyRing(List<Coordinate> ring) {
        boolean closed = !ring.isEmpty() && ring.get(0).equals(ring.get(ring.size() - 1));
        List<Coordinate> points = closed ? ring.subList(0, ring.size() - 1) : ring;
        List<Coordinate> simplified = douglasPeucker(points, toleranceDegrees);
        List<Coordinate> result = new ArrayList<>(simplified);
        if (closed) {
            result.add(result.get(0));
        }
        return result.stream().map(this::round).toList();
    }

    private List<Coordinate> douglasPeucker(List<Coordinate> points, double tolerance) {
        if (points.size() <= 2) {
            return new ArrayList<>(points);
        }
        boolean[] keep = new boolean[points.size()];
        keep[0] = true;
        keep[points.size() - 1] = true;
        simplifySegment(points, 0, points.size() - 1, tolerance, keep);

        List<Coordinate> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (keep[i]) {
                result.add(points.get(i));
            }
        }
        return result;
    }

    private void simplifySegment(List<Coordinate> points, int start, int end, double tolerance, boolean[] keep) {
        if (end <= start + 1) {
            return;
        }
        Coordinate a = points.get(start);
        Coordinate b = points.get(end);
        int maxIndex = -1;
        double maxDistance = 0.0;
        for (int i = start + 1; i < end; i++) {
            double distance = perpendicularDistance(points.get(i), a, b);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }
        if (maxDistance > tolerance) {
            keep[maxIndex] = true;
            simplifySegment(points, start, maxIndex, tolerance, keep);
            simplifySegment(points, maxIndex, end, tolerance, keep);
        }
    }

    private double perpendicularDistance(Coordinate point, Coordinate a, Coordinate b) {
        double dx = b.lon() - a.lon();
        double dy = b.lat() - a.lat();
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(point.lon() - a.lon(), point.lat() - a.lat());
        }
        double numerator = Math.abs(dy * point.lon() - dx * point.lat() + b.lon() * a.lat() - b.lat() * a.lon());
        return numerator / Math.hypot(dx, dy);
    }

    private Coordinate round(Coordinate coordinate) {
        double factor = Math.pow(10.0, decimalPlaces);
        return new Coordinate(Math.round(coordinate.lon() * factor) / factor,
                Math.round(coordinate.lat() * factor) / factor);
    }
}
