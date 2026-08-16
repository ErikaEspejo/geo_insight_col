package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

import java.util.Objects;

/**
 * Entidad más cercana a una coordenada con su distancia en metros. La regla de
 * empate es el orden de inserción estable del dataset (research.md §5).
 */
public record NearestEntity(GeoscienceEntity entity, double distanceMeters) {

    public NearestEntity {
        Objects.requireNonNull(entity, "entidad requerida");
        if (Double.isNaN(distanceMeters) || distanceMeters < 0.0) {
            throw new IllegalArgumentException("La distancia debe ser un valor no negativo");
        }
    }
}
