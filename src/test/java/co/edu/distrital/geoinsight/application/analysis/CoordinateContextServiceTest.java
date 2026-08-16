package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.repository.DatasetRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoordinateContextServiceTest {

    private static final Coordinate CENTER = new Coordinate(-74, 4);

    @Test
    void outsideLoadedTectonicCoverageReturnsExplicitAbsence() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        DatasetRepository datasets = mock(DatasetRepository.class);
        when(datasets.isDatasetLoaded(Domain.DOMINIO_TECTONICO)).thenReturn(true);
        for (Domain domain : Domain.values()) when(catalog.findByDomain(domain)).thenReturn(List.of());

        CoordinateContext result = new CoordinateContextService(catalog, datasets, point -> true).context(CENTER);

        assertThat(result.insideCoverage()).isFalse();
        assertThat(result.geologicalUnits()).isEmpty();
        assertThat(result.nearestFault()).isNull();
        assertThat(result.nearestMassMovement()).isNull();
        assertThat(result.nearestVolcano()).isNull();
    }

    @Test
    void coverageFallsBackFromUnavailableTectonicDomainsToGeologicalUnits() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        DatasetRepository datasets = mock(DatasetRepository.class);
        GeoscienceEntity unit = polygon("unit", Domain.UNIDAD_GEOLOGICA);
        when(datasets.isDatasetLoaded(Domain.DOMINIO_TECTONICO)).thenReturn(false);
        when(datasets.isDatasetLoaded(Domain.UNIDAD_GEOLOGICA)).thenReturn(true);
        when(catalog.findByDomain(Domain.UNIDAD_GEOLOGICA)).thenReturn(List.of(unit));
        for (Domain domain : List.of(Domain.DOMINIO_TECTONICO, Domain.FALLA_GEOLOGICA,
                Domain.MOVIMIENTO_EN_MASA, Domain.VOLCAN)) when(catalog.findByDomain(domain)).thenReturn(List.of());

        CoordinateContext result = new CoordinateContextService(catalog, datasets, point -> false).context(CENTER);

        assertThat(result.insideCoverage()).isTrue();
        assertThat(result.geologicalUnits()).containsExactly(unit);
    }

    @Test
    void coverageFallsBackToBasemapOnlyWhenBothPolygonDatasetsAreUnavailable() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        DatasetRepository datasets = mock(DatasetRepository.class);
        when(datasets.isDatasetLoaded(Domain.DOMINIO_TECTONICO)).thenReturn(false);
        when(datasets.isDatasetLoaded(Domain.UNIDAD_GEOLOGICA)).thenReturn(false);
        for (Domain domain : Domain.values()) when(catalog.findByDomain(domain)).thenReturn(List.of());

        CoordinateContext result = new CoordinateContextService(catalog, datasets, point -> true).context(CENTER);

        assertThat(result.insideCoverage()).isTrue();
    }

    @Test
    void equalDistancesAreResolvedByFullIdentifierAscending() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        DatasetRepository datasets = mock(DatasetRepository.class);
        when(datasets.isDatasetLoaded(Domain.DOMINIO_TECTONICO)).thenReturn(true);
        when(catalog.findByDomain(Domain.DOMINIO_TECTONICO)).thenReturn(List.of(polygon("coverage", Domain.DOMINIO_TECTONICO)));
        when(catalog.findByDomain(Domain.UNIDAD_GEOLOGICA)).thenReturn(List.of());
        GeoscienceEntity later = point("SGC-Z", Domain.VOLCAN, -73, 4);
        GeoscienceEntity first = point("GEO-A", Domain.VOLCAN, -73, 4);
        when(catalog.findByDomain(Domain.VOLCAN)).thenReturn(List.of(later, first));
        when(catalog.findByDomain(Domain.FALLA_GEOLOGICA)).thenReturn(List.of());
        when(catalog.findByDomain(Domain.MOVIMIENTO_EN_MASA)).thenReturn(List.of());

        CoordinateContext result = new CoordinateContextService(catalog, datasets, point -> false).context(CENTER);

        assertThat(result.nearestVolcano().entity().id()).isEqualTo("GEO-A");
    }

    private static GeoscienceEntity polygon(String id, Domain domain) {
        return new GeoscienceEntity(id, domain, Origin.SGC, GeometryFactory.polygon(List.of(
                new Coordinate(-75, 3), new Coordinate(-73, 3), new Coordinate(-73, 5),
                new Coordinate(-75, 5), new Coordinate(-75, 3)), List.of()), Map.of());
    }

    private static GeoscienceEntity point(String id, Domain domain, double lon, double lat) {
        return new GeoscienceEntity(id, domain, Origin.SGC,
                GeometryFactory.point(new Coordinate(lon, lat)), Map.of());
    }
}
