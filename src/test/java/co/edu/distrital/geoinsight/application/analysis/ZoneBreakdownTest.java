package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneBreakdownTest {

    private GeoscienceEntity movement(String tipo) {
        Map<String, Object> attributes = tipo == null ? Map.of() : Map.of("TIPO", tipo);
        return new GeoscienceEntity("MOV-" + System.nanoTime(), Domain.MOVIMIENTO_EN_MASA, Origin.SGC,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)), attributes);
    }

    @Test
    void movementDistributionsClassifyBlankValuesAsUnclassified() {
        List<GeoscienceEntity> entities = List.of(
                movement("Deslizamiento"), movement("Deslizamiento"), movement("Caida"),
                movement(""), movement("   "), movement(null));
        ZoneBreakdown breakdown = ZoneBreakdown.movements(entities, true);
        assertThat(breakdown.count()).isEqualTo(6);
        assertThat(breakdown.byTipo())
                .containsEntry("Deslizamiento", 2L)
                .containsEntry("Caida", 1L)
                .containsEntry("Sin clasificar", 3L);
        assertThat(breakdown.bySubtipo()).containsEntry("Sin clasificar", 6L);
        assertThat(breakdown.byClasMapa()).containsEntry("Sin clasificar", 6L);
    }

    @Test
    void movementDistributionsSumToTotalCount() {
        List<GeoscienceEntity> entities = List.of(
                movement("Deslizamiento"), movement("Caida"), movement("Caida"), movement("Flujo"));
        ZoneBreakdown breakdown = ZoneBreakdown.movements(entities, true);
        long sum = breakdown.byTipo().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sum).isEqualTo(breakdown.count());
    }

    private GeoscienceEntity entity(String attributesValue) {
        return new GeoscienceEntity("E-" + System.nanoTime(), Domain.FALLA_GEOLOGICA, Origin.SGC,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)),
                attributesValue == null ? Map.of() : Map.of("Tipo", attributesValue));
    }

    @Test
    void faultBreakdownClassifiesByTipoAttribute() {
        List<GeoscienceEntity> entities = List.of(
                entity("Falla"), entity("Falla"), entity("Lineamiento"), entity(null));
        ZoneBreakdown breakdown = ZoneBreakdown.faults(entities, true);
        assertThat(breakdown.count()).isEqualTo(4);
        assertThat(breakdown.byTipo())
                .containsEntry("Falla", 2L)
                .containsEntry("Lineamiento", 1L)
                .containsEntry("Sin clasificar", 1L);
        assertThat(breakdown.bySubtipo()).isNull();
        assertThat(breakdown.byClasMapa()).isNull();
    }

    @Test
    void geologicalUnitsClassifyByEdadAndTectonicDomainsByNombre() {
        GeoscienceEntity jurassic = new GeoscienceEntity("U-1", Domain.UNIDAD_GEOLOGICA, Origin.SGC,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)), Map.of("Edad", "Jurásico"));
        GeoscienceEntity quaternary = new GeoscienceEntity("U-2", Domain.UNIDAD_GEOLOGICA, Origin.SGC,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)), Map.of("Edad", "Cuaternario"));
        GeoscienceEntity amazon = new GeoscienceEntity("T-1", Domain.DOMINIO_TECTONICO, Origin.SGC,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)), Map.of("NombreDT", "Basamento Amazónico"));

        ZoneBreakdown units = ZoneBreakdown.geologicalUnits(List.of(jurassic, quaternary), true);
        assertThat(units.byTipo())
                .containsEntry("Jurásico", 1L)
                .containsEntry("Cuaternario", 1L);

        ZoneBreakdown domains = ZoneBreakdown.tectonicDomains(List.of(amazon), true);
        assertThat(domains.byTipo()).containsEntry("Basamento Amazónico", 1L);
        assertThat(domains.bySubtipo()).isNull();
    }
}
