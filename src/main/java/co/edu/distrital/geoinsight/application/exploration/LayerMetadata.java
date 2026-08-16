package co.edu.distrital.geoinsight.application.exploration;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metadatos de una capa del mapa: atributos reales del dataset con sus valores
 * distintos (FR-008, FR-018) y conteo combinado SGC + GEOINSIGHT.
 */
public record LayerMetadata(
        Domain domain,
        String displayName,
        String geometryType,
        int count,
        Map<String, List<String>> filterableAttributes,
        Set<String> requiredAttributes,
        Set<String> editableAttributes,
        Map<String, AttributeValueType> editableAttributeTypes,
        boolean dataAvailable) {
}
