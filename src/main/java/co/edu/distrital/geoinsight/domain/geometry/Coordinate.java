package co.edu.distrital.geoinsight.domain.geometry;

/**
 * Coordenada geográfica en WGS84 (longitud, latitud en grados decimales).
 * Inmutable; valida rangos y rechaza valores no numéricos.
 */
public record Coordinate(double lon, double lat) {

    public Coordinate {
        if (Double.isNaN(lon) || Double.isNaN(lat) || Double.isInfinite(lon) || Double.isInfinite(lat)) {
            throw new IllegalArgumentException("Coordenadas deben ser valores numéricos finitos");
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Longitud fuera de rango: " + lon);
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Latitud fuera de rango: " + lat);
        }
    }

    public double distanceMeters(Coordinate other) {
        return GeoMath.distanceMeters(this, other);
    }
}
