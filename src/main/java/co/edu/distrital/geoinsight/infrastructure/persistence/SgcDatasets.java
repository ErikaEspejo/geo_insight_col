package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Domain;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fuentes oficiales de datos SGC verificadas (research.md §1). Los conteos
 * esperados y tamaños de página fueron confirmados contra la API REST.
 */
public final class SgcDatasets {

    public record Source(String fileName, String serviceUrl, int layer, long expectedCount, int pageSize, Domain domain) {
    }

    public static final List<Source> SOURCES = List.of(
            new Source("Volcanes.geojson",
                    "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer",
                    0, 61, 1000, Domain.VOLCAN),
            new Source("Fallas.geojson",
                    "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer",
                    1, 4866, 1000, Domain.FALLA_GEOLOGICA),
            new Source("Mapa_Geologico_de_Colombia_2015.geojson",
                    "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/MAPAGEOLOGIA/FeatureServer",
                    4, 7461, 1000, Domain.UNIDAD_GEOLOGICA),
            new Source("Mapa_Tectonico_de_Colombia_2017.geojson",
                    "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/Mapa_Tect%C3%B3nico_de_Colombia_2017_Dominios_Tect%C3%B3nicosDominios_Tect%C3%B3nicos/FeatureServer",
                    0, 3, 2000, Domain.DOMINIO_TECTONICO),
            new Source("Inventario_de_movimientos_en_masa.geojson",
                    "https://services1.arcgis.com/Og2nrTKe5bptW02d/arcgis/rest/services/Inventario_de_movimientos_en_masa/FeatureServer",
                    0, 6826, 2000, Domain.MOVIMIENTO_EN_MASA)
    );

    public static final Map<Domain, Source> BY_DOMAIN = SOURCES.stream()
            .collect(Collectors.toUnmodifiableMap(Source::domain, Function.identity()));

    private SgcDatasets() {
    }
}
