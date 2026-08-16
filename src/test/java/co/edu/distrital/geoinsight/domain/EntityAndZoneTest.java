package co.edu.distrital.geoinsight.domain;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.model.Zone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityAndZoneTest {

    @Test
    void entityKeepsDomainOriginAndAttributesImmutable() {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("TIPO", "D");
        GeoscienceEntity entity = new GeoscienceEntity("GEO-1", Domain.MOVIMIENTO_EN_MASA, Origin.GEOINSIGHT,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)), attributes);
        assertThat(entity.origin()).isEqualTo(Origin.GEOINSIGHT);
        assertThat(entity.domain()).isEqualTo(Domain.MOVIMIENTO_EN_MASA);
        assertThat(entity.attributes().get("TIPO")).isEqualTo("D");
        assertThatThrownBy(() -> entity.attributes().put("X", 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void zoneRejectsNonPositiveRadius() {
        Coordinate center = new Coordinate(-74.0, 4.7);
        assertThatThrownBy(() -> new Zone(center, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Zone(center, -5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zoneRejectsNonFiniteRadius() {
        Coordinate center = new Coordinate(-74.0, 4.7);
        assertThatThrownBy(() -> new Zone(center, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Zone(center, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Zone(center, Double.NEGATIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zoneContainsCoordinateWithinRadius() {
        Zone zone = new Zone(new Coordinate(-74.0, 4.7), 112_000);
        assertThat(zone.containsCoordinate(new Coordinate(-74.0, 5.7))).isTrue();
        assertThat(zone.containsCoordinate(new Coordinate(-74.0, 10.0))).isFalse();
    }
}
