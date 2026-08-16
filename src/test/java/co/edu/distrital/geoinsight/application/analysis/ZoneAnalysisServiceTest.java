package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.application.common.EntityCatalog;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.model.Zone;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZoneAnalysisServiceTest {

    @Test
    void includesOnlyEntitiesWhoseGeometryReachesTheRadius() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        Coordinate center = new Coordinate(-74, 4);
        GeoscienceEntity inside = point("inside", Domain.MOVIMIENTO_EN_MASA, -74.01, 4);
        GeoscienceEntity outside = point("outside", Domain.MOVIMIENTO_EN_MASA, -75, 4);
        when(catalog.findByDomain(Domain.MOVIMIENTO_EN_MASA)).thenReturn(List.of(inside, outside));
        for (Domain domain : List.of(Domain.FALLA_GEOLOGICA, Domain.UNIDAD_GEOLOGICA,
                Domain.DOMINIO_TECTONICO, Domain.VOLCAN)) when(catalog.findByDomain(domain)).thenReturn(List.of());
        for (Domain domain : Domain.values()) when(catalog.dataAvailable(domain)).thenReturn(true);

        ZoneAnalysisResult result = new ZoneAnalysisService(catalog).analyze(new Zone(center, 2_000));

        assertThat(result.massMovements().entities()).containsExactly(inside);
        assertThat(result.massMovements().count()).isEqualTo(1);
    }

    @Test
    void preservesDatasetUnavailableStateSeparatelyFromZeroMatches() {
        EntityCatalog catalog = mock(EntityCatalog.class);
        for (Domain domain : Domain.values()) when(catalog.findByDomain(domain)).thenReturn(List.of());
        when(catalog.dataAvailable(Domain.VOLCAN)).thenReturn(false);
        when(catalog.dataAvailable(Domain.FALLA_GEOLOGICA)).thenReturn(true);

        ZoneAnalysisResult result = new ZoneAnalysisService(catalog)
                .analyze(new Zone(new Coordinate(-74, 4), 1_000));

        assertThat(result.volcanoes().dataAvailable()).isFalse();
        assertThat(result.faults().dataAvailable()).isTrue();
        assertThat(result.faults().count()).isZero();
    }

    private static GeoscienceEntity point(String id, Domain domain, double lon, double lat) {
        return new GeoscienceEntity(id, domain, Origin.SGC,
                GeometryFactory.point(new Coordinate(lon, lat)), Map.of("TIPO", "Prueba"));
    }
}
