package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

import java.util.List;
import java.util.Objects;

/**
 * Contexto geocientífico de una coordenada (FR-009): unidades y dominios que
 * la contienen y la entidad más cercana por dominio con su distancia. La
 * ausencia por dominio es explícita (FR-014).
 */
public record CoordinateContext(
        Coordinate coordinate,
        boolean insideCoverage,
        List<GeoscienceEntity> geologicalUnits,
        List<GeoscienceEntity> tectonicDomains,
        NearestEntity nearestFault,
        NearestEntity nearestMassMovement,
        NearestEntity nearestVolcano) {

    public CoordinateContext {
        Objects.requireNonNull(coordinate, "coordenada requerida");
        geologicalUnits = List.copyOf(geologicalUnits);
        tectonicDomains = List.copyOf(tectonicDomains);
    }
}
