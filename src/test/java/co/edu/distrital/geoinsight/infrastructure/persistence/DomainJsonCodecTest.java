package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.geometry.LineString;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DomainJsonCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void pointRoundTrip() {
        Geometry geometry = GeometryFactory.point(new Coordinate(-74.07, 4.71));
        JsonNode node = DomainJsonCodec.geometryToJson(objectMapper, geometry);
        Geometry parsed = DomainJsonCodec.geometryFromJson(node);
        assertThat(parsed.geoJsonType()).isEqualTo("Point");
        assertThat(parsed.distanceMeters(new Coordinate(-74.07, 4.71))).isZero();
    }

    @Test
    void polygonWithHoleRoundTrip() {
        Polygon polygon = GeometryFactory.polygon(
                List.of(new Coordinate(-2, -2), new Coordinate(2, -2), new Coordinate(2, 2), new Coordinate(-2, 2), new Coordinate(-2, -2)),
                List.of(List.of(new Coordinate(-1, -1), new Coordinate(1, -1), new Coordinate(1, 1), new Coordinate(-1, 1), new Coordinate(-1, -1))));
        Geometry parsed = DomainJsonCodec.geometryFromJson(DomainJsonCodec.geometryToJson(objectMapper, polygon));
        assertThat(parsed.contains(new Coordinate(0, 0))).isFalse();
        assertThat(parsed.contains(new Coordinate(1.5, 0))).isTrue();
    }

    @Test
    void entityRoundTripPreservesDomainOriginAndAttributes() {
        GeoscienceEntity entity = new GeoscienceEntity("GEO-1", Domain.VOLCAN, Origin.GEOINSIGHT,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)),
                Map.of("NombreVolcan", "Nuevo", "AlturaSobreNivelMar", "4300"));
        JsonNode node = DomainJsonCodec.entityToJson(objectMapper, entity);
        GeoscienceEntity parsed = DomainJsonCodec.entityFromJson(objectMapper, node);
        assertThat(parsed.id()).isEqualTo("GEO-1");
        assertThat(parsed.domain()).isEqualTo(Domain.VOLCAN);
        assertThat(parsed.origin()).isEqualTo(Origin.GEOINSIGHT);
        assertThat(parsed.attributes().get("NombreVolcan")).isEqualTo("Nuevo");
    }
}
