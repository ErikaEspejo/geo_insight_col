package co.edu.distrital.geoinsight.application.exploration;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.Point;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeometrySimplifierTest {

    private final GeometrySimplifier simplifier = new GeometrySimplifier(0.01, 4);

    @Test
    void keepsPointsUntouched() {
        Point point = GeometryFactory.point(new Coordinate(-74.07, 4.71));
        assertThat(simplifier.simplify(point)).isSameAs(point);
    }

    @Test
    void preservesClosedRings() {
        List<Coordinate> ring = List.of(
                new Coordinate(-74.0, 4.0), new Coordinate(-73.5, 4.5), new Coordinate(-73.0, 4.0),
                new Coordinate(-73.5, 3.5), new Coordinate(-74.0, 4.0));
        Polygon simplified = (Polygon) simplifier.simplify(GeometryFactory.polygon(ring, List.of()));

        assertThat(simplified.exteriorRing()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(simplified.exteriorRing().get(0)).isEqualTo(simplified.exteriorRing().get(
                simplified.exteriorRing().size() - 1));
    }

    @Test
    void removesCollinearPoints() {
        List<Coordinate> ring = List.of(
                new Coordinate(-74.0, 4.0), new Coordinate(-73.99, 4.0), new Coordinate(-73.98, 4.0),
                new Coordinate(-73.97, 4.0), new Coordinate(-74.0, 4.0));
        Polygon simplified = (Polygon) simplifier.simplify(GeometryFactory.polygon(ring, List.of()));

        assertThat(simplified.exteriorRing()).hasSizeLessThan(ring.size());
    }

    @Test
    void roundsCoordinatesToConfiguredPrecision() {
        Polygon simplified = (Polygon) simplifier.simplify(GeometryFactory.polygon(
                List.of(new Coordinate(-74.1234567, 4.1234567), new Coordinate(-73.1, 4.2),
                        new Coordinate(-73.3, 3.9), new Coordinate(-74.1234567, 4.1234567)), List.of()));

        Coordinate c = simplified.exteriorRing().get(0);
        assertThat(c.lon()).isEqualTo(-74.1235);
        assertThat(c.lat()).isEqualTo(4.1235);
    }

    @Test
    void simplifiesHolesToo() {
        List<Coordinate> hole = List.of(
                new Coordinate(-73.9, 4.1), new Coordinate(-73.89, 4.1), new Coordinate(-73.88, 4.1),
                new Coordinate(-73.9, 4.1));
        Polygon simplified = (Polygon) simplifier.simplify(GeometryFactory.polygon(
                List.of(new Coordinate(-74.0, 4.0), new Coordinate(-73.0, 4.5), new Coordinate(-73.0, 3.5),
                        new Coordinate(-74.0, 4.0)),
                List.of(hole)));

        assertThat(simplified.holes()).hasSize(1);
        assertThat(simplified.holes().get(0)).hasSizeLessThan(hole.size());
    }
}
