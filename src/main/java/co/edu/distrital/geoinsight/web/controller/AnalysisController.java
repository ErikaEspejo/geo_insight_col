package co.edu.distrital.geoinsight.web.controller;

import co.edu.distrital.geoinsight.application.analysis.CoordinateContext;
import co.edu.distrital.geoinsight.application.analysis.CoordinateContextService;
import co.edu.distrital.geoinsight.application.analysis.ComparedZone;
import co.edu.distrital.geoinsight.application.analysis.NearestEntity;
import co.edu.distrital.geoinsight.application.analysis.ZoneAnalysisResult;
import co.edu.distrital.geoinsight.application.analysis.ZoneAnalysisService;
import co.edu.distrital.geoinsight.application.analysis.ZoneBreakdown;
import co.edu.distrital.geoinsight.application.analysis.ZoneComparisonResult;
import co.edu.distrital.geoinsight.application.analysis.ZoneComparisonService;
import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Zone;
import co.edu.distrital.geoinsight.infrastructure.persistence.DomainJsonCodec;
import co.edu.distrital.geoinsight.web.dto.CompareRequest;
import co.edu.distrital.geoinsight.web.dto.CoordinateRequest;
import co.edu.distrital.geoinsight.web.dto.ZoneRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * US3/US4/US5 — Contexto de coordenada, análisis de zona y comparación.
 * Respuestas descriptivas, sin riesgo ni predicciones (FR-012).
 */
@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final CoordinateContextService contextService;
    private final ZoneAnalysisService zoneAnalysisService;
    private final ZoneComparisonService zoneComparisonService;
    private final ObjectMapper objectMapper;

    public AnalysisController(CoordinateContextService contextService, ZoneAnalysisService zoneAnalysisService,
                              ZoneComparisonService zoneComparisonService, ObjectMapper objectMapper) {
        this.contextService = contextService;
        this.zoneAnalysisService = zoneAnalysisService;
        this.zoneComparisonService = zoneComparisonService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/context")
    public ObjectNode context(@Valid @RequestBody CoordinateRequest request) {
        return contextToJson(contextService.context(toCoordinate(request)));
    }

    @PostMapping("/zones/analyze")
    public ObjectNode analyze(@Valid @RequestBody ZoneRequest request) {
        return zoneToJson(zoneAnalysisService.analyze(toZone(request)));
    }

    @PostMapping("/zones/compare")
    public ObjectNode compare(@Valid @RequestBody CompareRequest request) {
        ZoneComparisonResult result = zoneComparisonService.compare(toZone(request.zoneA()), toZone(request.zoneB()));
        ObjectNode node = objectMapper.createObjectNode();
        node.set("zoneA", comparedZoneToJson(result.zoneA()));
        node.set("zoneB", comparedZoneToJson(result.zoneB()));
        return node;
    }

    private ObjectNode comparedZoneToJson(ComparedZone compared) {
        ObjectNode node = zoneToJson(compared.analysis());
        CoordinateContext context = compared.centerContext();
        node.set("centerGeologicalUnits", entitiesToJson(context.geologicalUnits()));
        node.set("centerTectonicDomains", entitiesToJson(context.tectonicDomains()));
        node.set("nearestFault", nearestToJson(context.nearestFault()));
        node.set("nearestMassMovement", nearestToJson(context.nearestMassMovement()));
        node.set("nearestVolcano", nearestToJson(context.nearestVolcano()));
        return node;
    }

    private Coordinate toCoordinate(CoordinateRequest request) {
        return new Coordinate(request.lon(), request.lat());
    }

    private Zone toZone(ZoneRequest request) {
        return new Zone(new Coordinate(request.lon(), request.lat()), request.radiusMeters());
    }

    private ObjectNode contextToJson(CoordinateContext context) {
        ObjectNode node = objectMapper.createObjectNode();
        node.set("coordinate", coordinateToJson(context.coordinate()));
        node.set("geologicalUnits", entitiesToJson(context.geologicalUnits()));
        node.set("tectonicDomains", entitiesToJson(context.tectonicDomains()));
        node.set("nearestFault", nearestToJson(context.nearestFault()));
        node.set("nearestMassMovement", nearestToJson(context.nearestMassMovement()));
        node.set("nearestVolcano", nearestToJson(context.nearestVolcano()));
        return node;
    }

    private ObjectNode zoneToJson(ZoneAnalysisResult result) {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode zone = objectMapper.createObjectNode();
        zone.set("lon", objectMapper.valueToTree(result.zone().center().lon()));
        zone.set("lat", objectMapper.valueToTree(result.zone().center().lat()));
        zone.set("radiusMeters", objectMapper.valueToTree(result.zone().radiusMeters()));
        node.set("zone", zone);
        node.set("massMovements", breakdownToJson(result.massMovements()));
        node.set("faults", breakdownToJson(result.faults()));
        node.set("geologicalUnits", breakdownToJson(result.geologicalUnits()));
        node.set("tectonicDomains", breakdownToJson(result.tectonicDomains()));
        node.set("volcanoes", breakdownToJson(result.volcanoes()));
        return node;
    }

    private ObjectNode breakdownToJson(ZoneBreakdown breakdown) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("dataAvailable", breakdown.dataAvailable());
        node.set("count", objectMapper.valueToTree(breakdown.count()));
        if (breakdown.byTipo() != null) {
            node.set("byTipo", distributionToJson(breakdown.byTipo()));
        }
        if (breakdown.bySubtipo() != null) {
            node.set("bySubtipo", distributionToJson(breakdown.bySubtipo()));
        }
        if (breakdown.byClasMapa() != null) {
            node.set("byClasMapa", distributionToJson(breakdown.byClasMapa()));
        }
        node.set("entities", entitiesToJson(breakdown.entities()));
        return node;
    }

    private ObjectNode distributionToJson(Map<String, Long> distribution) {
        ObjectNode node = objectMapper.createObjectNode();
        distribution.forEach(node::put);
        return node;
    }

    private JsonNode nearestToJson(NearestEntity nearest) {
        if (nearest == null) {
            return objectMapper.nullNode();
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.set("entity", DomainJsonCodec.entityToJson(objectMapper, nearest.entity()));
        node.set("distanceMeters", objectMapper.valueToTree(nearest.distanceMeters()));
        return node;
    }

    private ObjectNode coordinateToJson(Coordinate coordinate) {
        ObjectNode node = objectMapper.createObjectNode();
        node.set("lon", objectMapper.valueToTree(coordinate.lon()));
        node.set("lat", objectMapper.valueToTree(coordinate.lat()));
        return node;
    }

    private ArrayNode entitiesToJson(List<GeoscienceEntity> entities) {
        ArrayNode array = objectMapper.createArrayNode();
        for (GeoscienceEntity entity : entities) {
            array.add(DomainJsonCodec.entityToJson(objectMapper, entity));
        }
        return array;
    }
}
