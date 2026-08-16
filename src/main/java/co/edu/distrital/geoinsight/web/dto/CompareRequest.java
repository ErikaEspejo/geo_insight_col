package co.edu.distrital.geoinsight.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CompareRequest(
        @NotNull(message = "La zona A es obligatoria") @Valid CoordinateRequest zoneA,
        @NotNull(message = "La zona B es obligatoria") @Valid CoordinateRequest zoneB,
        @NotNull(message = "El radio es obligatorio")
        @DecimalMin(value = "1", message = "El radio debe ser un valor positivo")
        Double radiusMeters) {
}
