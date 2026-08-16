package co.edu.distrital.geoinsight.domain.model;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;

import java.util.Objects;

/**
 * Zona de análisis: coordenada central y radio en metros (FR-010, FR-016).
 * El radio debe ser estrictamente positivo.
 */
public record Zone(Coordinate center, double radiusMeters) {

    public Zone {
        Objects.requireNonNull(center, "centro requerido");
        if (!Double.isFinite(radiusMeters) || radiusMeters <= 0.0) {
            throw new IllegalArgumentException("El radio debe ser un valor finito y positivo");
        }
    }

    public boolean containsCoordinate(Coordinate coordinate) {
        return center.distanceMeters(coordinate) <= radiusMeters;
    }
}
