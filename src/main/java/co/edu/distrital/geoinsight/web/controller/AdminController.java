package co.edu.distrital.geoinsight.web.controller;

import co.edu.distrital.geoinsight.application.admin.GeoEntityManagementService;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.infrastructure.persistence.DomainJsonCodec;
import co.edu.distrital.geoinsight.web.dto.AdminEntityRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * US6 — Administración de entidades GEOINSIGHT (FR-004, FR-005, FR-018).
 * Ruta restringida a rol ADMIN por SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final GeoEntityManagementService managementService;
    private final ObjectMapper objectMapper;

    public AdminController(GeoEntityManagementService managementService, ObjectMapper objectMapper) {
        this.managementService = managementService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/entities")
    public ArrayNode entities() {
        ArrayNode array = objectMapper.createArrayNode();
        for (GeoscienceEntity entity : managementService.findAll()) {
            array.add(DomainJsonCodec.entityToJson(objectMapper, entity));
        }
        return array;
    }

    @PostMapping("/entities")
    public ResponseEntity<ObjectNode> create(@Valid @RequestBody AdminEntityRequest request) {
        GeoscienceEntity entity = managementService.create(
                domain(request.domain()), geometry(request.geometry()), request.attributes());
        return ResponseEntity.status(HttpStatus.CREATED).body(DomainJsonCodec.entityToJson(objectMapper, entity));
    }

    @PutMapping("/entities/{id}")
    public ObjectNode update(@PathVariable String id, @Valid @RequestBody AdminEntityRequest request) {
        GeoscienceEntity entity = managementService.update(
                id, domain(request.domain()), geometry(request.geometry()), request.attributes());
        return DomainJsonCodec.entityToJson(objectMapper, entity);
    }

    @DeleteMapping("/entities/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        managementService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Domain domain(String key) {
        return Domain.fromKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Dominio desconocido: " + key));
    }

    private Geometry geometry(com.fasterxml.jackson.databind.JsonNode node) {
        return DomainJsonCodec.geometryFromJson(node);
    }
}
