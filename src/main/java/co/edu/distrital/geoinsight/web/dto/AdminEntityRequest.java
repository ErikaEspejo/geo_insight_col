package co.edu.distrital.geoinsight.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AdminEntityRequest(
        @NotBlank(message = "El dominio es obligatorio") String domain,
        @NotNull(message = "La geometría es obligatoria") JsonNode geometry,
        @NotNull(message = "Los atributos son obligatorios") Map<String, Object> attributes) {
}
