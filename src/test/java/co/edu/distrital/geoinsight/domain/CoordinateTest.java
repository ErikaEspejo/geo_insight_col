package co.edu.distrital.geoinsight.domain;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordinateTest {

    @Test
    void rejectsOutOfRangeValues() {
        assertThatThrownBy(() -> new Coordinate(181, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinate(0, -91)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordinate(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesHaversineDistanceInMeters() {
        Coordinate a = new Coordinate(0, 0);
        Coordinate b = new Coordinate(1, 0);
        double meters = a.distanceMeters(b);
        assertThat(meters).isBetween(110_900.0, 111_500.0);
    }

    @Test
    void distanceToItselfIsZero() {
        Coordinate a = new Coordinate(-74.07, 4.71);
        assertThat(a.distanceMeters(a)).isZero();
    }
}
