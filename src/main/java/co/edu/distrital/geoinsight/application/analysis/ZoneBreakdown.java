package co.edu.distrital.geoinsight.application.analysis;

import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Indicadores descriptivos de un dominio dentro de una zona: conteo, conteos
 * por atributos de distribución y las entidades. Los movimientos en masa se
 * distribuyen por TIPO, SUBTIPO y CLAS_MAPA (FR-010); las fallas por su tipo,
 * las unidades geológicas por edad y los dominios tectónicos por nombre. Sin
 * conclusiones de riesgo (FR-012).
 */
public record ZoneBreakdown(
        boolean dataAvailable,
        int count,
        Map<String, Long> byTipo,
        Map<String, Long> bySubtipo,
        Map<String, Long> byClasMapa,
        List<GeoscienceEntity> entities) {

    public ZoneBreakdown {
        entities = List.copyOf(entities);
    }

    public static ZoneBreakdown plain(List<GeoscienceEntity> entities, boolean dataAvailable) {
        return new ZoneBreakdown(dataAvailable, entities.size(), null, null, null, entities);
    }

    public static ZoneBreakdown movements(List<GeoscienceEntity> entities, boolean dataAvailable) {
        return new ZoneBreakdown(dataAvailable, entities.size(),
                distribution(entities, "TIPO"),
                distribution(entities, "SUBTIPO"),
                distribution(entities, "CLAS_MAPA"),
                entities);
    }

    public static ZoneBreakdown faults(List<GeoscienceEntity> entities, boolean dataAvailable) {
        return classified(entities, dataAvailable, "Tipo");
    }

    public static ZoneBreakdown geologicalUnits(List<GeoscienceEntity> entities, boolean dataAvailable) {
        return classified(entities, dataAvailable, "Edad");
    }

    public static ZoneBreakdown tectonicDomains(List<GeoscienceEntity> entities, boolean dataAvailable) {
        return classified(entities, dataAvailable, "NombreDT");
    }

    private static ZoneBreakdown classified(List<GeoscienceEntity> entities, boolean dataAvailable, String attribute) {
        return new ZoneBreakdown(dataAvailable, entities.size(), distribution(entities, attribute), null, null, entities);
    }

    private static final String UNCLASSIFIED = "Sin clasificar";

    private static Map<String, Long> distribution(List<GeoscienceEntity> entities, String attribute) {
        Map<String, Long> distribution = new TreeMap<>();
        for (GeoscienceEntity entity : entities) {
            String value = entity.attributeString(attribute)
                    .filter(text -> !text.isBlank())
                    .orElse(UNCLASSIFIED);
            distribution.merge(value, 1L, Long::sum);
        }
        return distribution;
    }
}
