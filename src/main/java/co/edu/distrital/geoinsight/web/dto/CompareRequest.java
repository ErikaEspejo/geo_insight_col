package co.edu.distrital.geoinsight.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompareRequest(
        @NotNull(message = "La zona A es obligatoria") @Valid ZoneRequest zoneA,
        @NotNull(message = "La zona B es obligatoria") @Valid ZoneRequest zoneB) {
}
