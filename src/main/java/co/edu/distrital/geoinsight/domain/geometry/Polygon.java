package co.edu.distrital.geoinsight.domain.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Polígono: anillo exterior (cerrado según GeoJSON, tolerando anillos abiertos)
 * y opcionalmente agujeros. La contención excluye los agujeros.
 */
public final class Polygon extends Geometry {

    private final List<Coordinate> exteriorRing;
    private final List<List<Coordinate>> holes;

    public Polygon(List<Coordinate> exteriorRing, List<List<Coordinate>> holes) {
        if (exteriorRing == null || exteriorRing.size() < 3) {
            throw new IllegalArgumentException("Polygon requiere un anillo exterior con al menos 3 coordenadas");
        }
        this.exteriorRing = closeRing(exteriorRing);
        this.holes = holes == null ? List.of() : holes.stream().map(Polygon::closeRing).toList();
    }

    public List<Coordinate> exteriorRing() {
        return exteriorRing;
    }

    public List<List<Coordinate>> holes() {
        return holes;
    }

    @Override
    public String geoJsonType() {
        return "Polygon";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        if (contains(point)) {
            return 0.0;
        }
        double min = Double.POSITIVE_INFINITY;
        for (List<Coordinate> ring : allRings()) {
            for (int i = 0; i < ring.size() - 1; i++) {
                min = Math.min(min, GeoMath.distancePointToSegmentMeters(point, ring.get(i), ring.get(i + 1)));
            }
        }
        return min;
    }

    @Override
    public boolean contains(Coordinate point) {
        if (!GeoMath.pointInRing(point, exteriorRing)) {
            return false;
        }
        for (List<Coordinate> hole : holes) {
            if (GeoMath.pointInRing(point, hole)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Bounds bounds() {
        return LineString.boundsOf(exteriorRing);
    }

    private List<List<Coordinate>> allRings() {
        List<List<Coordinate>> rings = new ArrayList<>(holes.size() + 1);
        rings.add(exteriorRing);
        rings.addAll(holes);
        return rings;
    }

    private static List<Coordinate> closeRing(List<Coordinate> ring) {
        if (ring == null || ring.size() < 3) {
            throw new IllegalArgumentException("Cada anillo de Polygon requiere al menos 3 coordenadas");
        }
        if (ring.getFirst().equals(ring.getLast())) {
            return List.copyOf(ring);
        }
        List<Coordinate> closed = new ArrayList<>(ring.size() + 1);
        closed.addAll(ring);
        closed.add(ring.getFirst());
        return List.copyOf(closed);
    }
}
