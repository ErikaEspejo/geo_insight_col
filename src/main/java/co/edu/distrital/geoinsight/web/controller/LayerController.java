package co.edu.distrital.geoinsight.web.controller;

import co.edu.distrital.geoinsight.application.exploration.LayerExplorationService;
import co.edu.distrital.geoinsight.application.exploration.LayerMetadata;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.web.dto.AttributeInfo;
import co.edu.distrital.geoinsight.web.dto.GeoJsonBuilder;
import co.edu.distrital.geoinsight.web.dto.LayerResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * US2 — Metadatos de capas y GeoJSON de visualización por dominio para
 * Leaflet. Las capas de polígonos pesadas se sirven con geometría simplificada
 * SOLO para render (contingencia SC-001); el análisis usa siempre la completa.
 */
@RestController
@RequestMapping("/api/layers")
public class LayerController {

    private final LayerExplorationService explorationService;
    private final ObjectMapper objectMapper;

    public LayerController(LayerExplorationService explorationService, ObjectMapper objectMapper) {
        this.explorationService = explorationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<LayerResponse> layers() {
        return explorationService.layers().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{domain}/geojson")
    public ObjectNode geojson(@PathVariable String domain) {
        Domain domainKey = domainFromKey(domain);
        List<GeoscienceEntity> entities = explorationService.entities(domainKey, Map.of());
        return GeoJsonBuilder.entitiesToFeatureCollection(objectMapper, entities,
                explorationService.visualizationTransform(domainKey));
    }

    private LayerResponse toResponse(LayerMetadata metadata) {
        List<AttributeInfo> attributes = metadata.filterableAttributes().entrySet().stream()
                .map(entry -> new AttributeInfo(entry.getKey(), entry.getValue()))
                .toList();
        return new LayerResponse(metadata.domain().name(), metadata.displayName(),
                metadata.geometryType(), metadata.count(), attributes,
                metadata.requiredAttributes().stream().sorted().toList(),
                metadata.editableAttributes().stream().toList(),
                metadata.editableAttributeTypes().entrySet().stream().collect(
                        java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().name(),
                                (left, right) -> left, java.util.LinkedHashMap::new)),
                metadata.dataAvailable());
    }

    private Domain domainFromKey(String key) {
        return Domain.fromKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Dominio desconocido: " + key));
    }
}
