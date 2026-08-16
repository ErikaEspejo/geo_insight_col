package co.edu.distrital.geoinsight.domain.repository;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;

/** Límite territorial local usado como último respaldo de cobertura. */
public interface CountryBoundary {
    boolean contains(Coordinate coordinate);
}
