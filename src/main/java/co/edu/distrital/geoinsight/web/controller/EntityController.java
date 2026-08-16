package co.edu.distrital.geoinsight.web.controller;

import co.edu.distrital.geoinsight.application.exploration.LayerExplorationService;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.infrastructure.persistence.DomainJsonCodec;
import co.edu.distrital.geoinsight.web.dto.GeoJsonBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US2 — Entidades filtradas por atributos reales (FR-008) y detalle con
 * procedencia (FR-015).
 */
@RestController
@RequestMapping("/api/entities")
public class EntityController {

    private final LayerExplorationService explorationService;
    private final ObjectMapper objectMapper;

    public EntityController(LayerExplorationService explorationService, ObjectMapper objectMapper) {
        this.explorationService = explorationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{domain}")
    public ObjectNode entities(@PathVariable String domain, @RequestParam MultiValueMap<String, String> params) {
        Domain domainKey = domainFromKey(domain);
        Map<String, List<String>> filters = new HashMap<>();
        params.forEach((attribute, values) -> filters.put(attribute, List.copyOf(values)));
        List<GeoscienceEntity> entities = explorationService.entitiesWithFilters(domainKey, filters);
        return GeoJsonBuilder.entitiesToFeatureCollection(objectMapper, entities,
                explorationService.visualizationTransform(domainKey));
    }

    @GetMapping("/{domain}/{id}")
    public ResponseEntity<ObjectNode> entity(@PathVariable String domain, @PathVariable String id) {
        Domain domainKey = domainFromKey(domain);
        return explorationService.entity(domainKey, id)
                .map(entity -> ResponseEntity.ok(DomainJsonCodec.entityToJson(objectMapper, entity)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Domain domainFromKey(String key) {
        return Domain.fromKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Dominio desconocido: " + key));
    }
}
