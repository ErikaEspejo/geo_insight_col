package co.edu.distrital.geoinsight.application.analysis;

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

class ZoneComparisonServiceTest {

    @Test
    void comparesTwoDifferentZonesAndSelectsEachZonesNearestEntity() {
        ZoneAnalysisService analysisService = mock(ZoneAnalysisService.class);
        Zone zoneA = new Zone(new Coordinate(-74, 4), 1_000);
        Zone zoneB = new Zone(new Coordinate(-75, 5), 1_000);
        GeoscienceEntity fartherFromA = point("A-2", Domain.VOLCAN, -74.005, 4);
        GeoscienceEntity nearerToA = point("A-1", Domain.VOLCAN, -74.001, 4);
        GeoscienceEntity nearerToB = point("B-1", Domain.VOLCAN, -75.001, 5);
        ZoneAnalysisResult analysisA = result(zoneA, List.of(fartherFromA, nearerToA));
        ZoneAnalysisResult analysisB = result(zoneB, List.of(nearerToB));
        when(analysisService.analyze(zoneA)).thenReturn(analysisA);
        when(analysisService.analyze(zoneB)).thenReturn(analysisB);

        ZoneComparisonResult comparison = new ZoneComparisonService(analysisService).compare(zoneA, zoneB);

        assertThat(comparison.zoneA().nearestVolcano().entity()).isEqualTo(nearerToA);
        assertThat(comparison.zoneA().analysis()).isSameAs(analysisA);
        assertThat(comparison.zoneB().nearestVolcano().entity()).isEqualTo(nearerToB);
        assertThat(comparison.zoneB().analysis()).isSameAs(analysisB);
    }

    @Test
    void emptyRadiusProducesNoNearestEntity() {
        ZoneAnalysisService analysisService = mock(ZoneAnalysisService.class);
        Zone zone = new Zone(new Coordinate(-74, 4), 1_000);
        when(analysisService.analyze(zone)).thenReturn(result(zone, List.of()));

        ComparedZone compared = new ZoneComparisonService(analysisService).compare(zone, zone).zoneA();

        assertThat(compared.nearestMassMovement()).isNull();
        assertThat(compared.nearestFault()).isNull();
        assertThat(compared.nearestVolcano()).isNull();
    }

    private static ZoneAnalysisResult result(Zone zone, List<GeoscienceEntity> volcanoes) {
        ZoneBreakdown empty = ZoneBreakdown.plain(List.of(), true);
        return new ZoneAnalysisResult(zone, ZoneBreakdown.movements(List.of(), true),
                ZoneBreakdown.faults(List.of(), true), ZoneBreakdown.geologicalUnits(List.of(), true),
                ZoneBreakdown.tectonicDomains(List.of(), true), ZoneBreakdown.plain(volcanoes, true));
    }

    private static GeoscienceEntity point(String id, Domain domain, double lon, double lat) {
        return new GeoscienceEntity(id, domain, Origin.SGC,
                GeometryFactory.point(new Coordinate(lon, lat)), Map.of());
    }
}
