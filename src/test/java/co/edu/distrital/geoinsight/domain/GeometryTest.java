package co.edu.distrital.geoinsight.domain;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.LineString;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeometryTest {

    private static Coordinate c(double lon, double lat) {
        return new Coordinate(lon, lat);
    }

    @Test
    void lineDistanceToParallelPoint() {
        LineString line = GeometryFactory.lineString(List.of(c(-1, 0), c(1, 0)));
        double meters = line.distanceMeters(c(0, 1));
        assertThat(meters).isBetween(110_900.0, 111_500.0);
    }

    @Test
    void lineDistanceToEndpoint() {
        LineString line = GeometryFactory.lineString(List.of(c(-1, 0), c(1, 0)));
        assertThat(line.distanceMeters(c(2, 0))).isBetween(110_900.0, 111_500.0);
    }

    @Test
    void polygonContainsCenterButNotOutside() {
        Polygon square = square(c(-1, -1), c(1, 1));
        assertThat(square.contains(c(0, 0))).isTrue();
        assertThat(square.contains(c(1.5, 0))).isFalse();
        assertThat(square.distanceMeters(c(2, 0))).isBetween(110_900.0, 111_500.0);
    }

    @Test
    void polygonExcludesHole() {
        Polygon withHole = GeometryFactory.polygon(
                ring(c(-2, -2), c(2, -2), c(2, 2), c(-2, 2), c(-2, -2)),
                List.of(ring(c(-1, -1), c(1, -1), c(1, 1), c(-1, 1), c(-1, -1))));
        assertThat(withHole.contains(c(0, 0))).isFalse();
        assertThat(withHole.contains(c(1.5, 0))).isTrue();
    }

    @Test
    void polygonClosesOpenExteriorAndHoleRings() {
        Polygon polygon = GeometryFactory.polygon(
                ring(c(-2, -2), c(2, -2), c(2, 2), c(-2, 2)),
                List.of(ring(c(-1, -1), c(1, -1), c(1, 1), c(-1, 1))));

        assertThat(polygon.exteriorRing()).hasSize(5);
        assertThat(polygon.exteriorRing().getFirst()).isEqualTo(polygon.exteriorRing().getLast());
        assertThat(polygon.holes().getFirst()).hasSize(5);
        assertThat(polygon.holes().getFirst().getFirst()).isEqualTo(polygon.holes().getFirst().getLast());
        assertThat(polygon.contains(c(0, 0))).isFalse();
        assertThat(polygon.distanceMeters(c(-3, 0))).isBetween(110_900.0, 111_500.0);
    }

    @Test
    void multiPolygonContainsAnyPart() {
        Geometry multi = GeometryFactory.multiPolygon(List.of(
                square(c(-3, -3), c(-2, -2)),
                square(c(2, 2), c(3, 3))));
        assertThat(multi.contains(c(-2.5, -2.5))).isTrue();
        assertThat(multi.contains(c(2.5, 2.5))).isTrue();
        assertThat(multi.contains(c(0, 0))).isFalse();
        assertThat(multi.distanceMeters(c(0, 0))).isBetween(310_000.0, 320_000.0);
    }

    @Test
    void multiLineDistanceIsMinimum() {
        Geometry multi = GeometryFactory.multiLineString(List.of(
                GeometryFactory.lineString(List.of(c(-10, -10), c(-9, -10))),
                GeometryFactory.lineString(List.of(c(5, 5), c(6, 5)))));
        assertThat(multi.distanceMeters(c(6, 6))).isBetween(110_900.0, 111_500.0);
    }

    private static Polygon square(Coordinate min, Coordinate max) {
        return GeometryFactory.polygon(
                ring(c(min.lon(), min.lat()), c(max.lon(), min.lat()), c(max.lon(), max.lat()), c(min.lon(), max.lat()), c(min.lon(), min.lat())),
                List.of());
    }

    private static List<Coordinate> ring(Coordinate... coordinates) {
        return List.of(coordinates);
    }
}
