package co.edu.distrital.geoinsight.domain.geometry;

/** Bounding box (WGS84) de una geometría. Inmutable. */
public record Bounds(double minLon, double minLat, double maxLon, double maxLat) {

    public boolean contains(Coordinate coordinate) {
        return coordinate.lon() >= minLon && coordinate.lon() <= maxLon
                && coordinate.lat() >= minLat && coordinate.lat() <= maxLat;
    }
}
