package co.edu.distrital.geoinsight.domain.geometry;

import java.util.List;

/**
 * Geometría base de las entidades geocientíficas. Toda geometría sabe calcular
 * la distancia mínima a un punto, indicar si contiene un punto y su bbox.
 */
public abstract class Geometry {

    public abstract String geoJsonType();

    public abstract double distanceMeters(Coordinate point);

    public abstract boolean contains(Coordinate point);

    public abstract Bounds bounds();

    protected static List<Coordinate> requireAtLeast(List<Coordinate> coordinates, int minimum, String kind) {
        if (coordinates == null || coordinates.size() < minimum) {
            throw new IllegalArgumentException(kind + " requiere al menos " + minimum + " coordenadas");
        }
        return List.copyOf(coordinates);
    }
}
