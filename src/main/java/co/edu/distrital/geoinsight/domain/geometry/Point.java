package co.edu.distrital.geoinsight.domain.geometry;

/** Geometría puntual (un par de coordenadas). */
public final class Point extends Geometry {

    private final Coordinate coordinate;

    public Point(Coordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("Point requiere una coordenada");
        }
        this.coordinate = coordinate;
    }

    public Coordinate coordinate() {
        return coordinate;
    }

    @Override
    public String geoJsonType() {
        return "Point";
    }

    @Override
    public double distanceMeters(Coordinate point) {
        return coordinate.distanceMeters(point);
    }

    @Override
    public boolean contains(Coordinate point) {
        return coordinate.equals(point);
    }

    @Override
    public Bounds bounds() {
        return new Bounds(coordinate.lon(), coordinate.lat(), coordinate.lon(), coordinate.lat());
    }
}
