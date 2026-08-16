package co.edu.distrital.geoinsight.domain.geometry;

/**
 * Utilidades de matemática geoespacial. Semántica comprobable (research.md §6):
 * haversine para distancias punto-punto, proyección planar local para
 * punto-segmento y ray casting para contención en anillos.
 */
final class GeoMath {

    private static final double EARTH_RADIUS_METERS = 6_371_008.8;
    private static final double METERS_PER_DEGREE_LAT = 111_320.0;

    private GeoMath() {
    }

    static double distanceMeters(Coordinate a, Coordinate b) {
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLon = Math.toRadians(b.lon() - a.lon());
        double sinLat = Math.sin(dLat / 2.0);
        double sinLon = Math.sin(dLon / 2.0);
        double h = sinLat * sinLat
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat())) * sinLon * sinLon;
        return 2.0 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
    }

    /**
     * Distancia mínima entre un punto y un segmento, aproximando la geometría
     * local por un plano tangente centrado en el punto. Suficientemente exacta
     * para distancias de proximidad entre geometrías cercanas.
     */
    static double distancePointToSegmentMeters(Coordinate point, Coordinate a, Coordinate b) {
        double metersPerDegLon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(point.lat()));
        double ax = (a.lon() - point.lon()) * metersPerDegLon;
        double ay = (a.lat() - point.lat()) * METERS_PER_DEGREE_LAT;
        double bx = (b.lon() - point.lon()) * metersPerDegLon;
        double by = (b.lat() - point.lat()) * METERS_PER_DEGREE_LAT;
        double dx = bx - ax;
        double dy = by - ay;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0.0) {
            return Math.hypot(ax, ay);
        }
        double t = Math.max(0.0, Math.min(1.0, -(ax * dx + ay * dy) / lengthSquared));
        double cx = ax + t * dx;
        double cy = ay + t * dy;
        return Math.hypot(cx, cy);
    }

    /** Ray casting: determina si un punto está dentro de un anillo (ciclo cerrado). */
    static boolean pointInRing(Coordinate point, java.util.List<Coordinate> ring) {
        boolean inside = false;
        int n = ring.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = ring.get(i).lon();
            double yi = ring.get(i).lat();
            double xj = ring.get(j).lon();
            double yj = ring.get(j).lat();
            boolean crosses = ((yi > point.lat()) != (yj > point.lat()))
                    && (point.lon() < (xj - xi) * (point.lat() - yi) / (yj - yi) + xi);
            if (crosses) {
                inside = !inside;
            }
        }
        return inside;
    }
}
