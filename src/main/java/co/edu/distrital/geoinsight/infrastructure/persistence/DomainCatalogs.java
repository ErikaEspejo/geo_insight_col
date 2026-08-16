package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Domain;

import java.util.List;
import java.util.Map;

/**
 * Diccionarios de datos derivados de la inspección de los datasets reales
 * (research.md §2, FR-008, FR-018). No se inventan atributos: si un atributo
 * no aparece en el dataset, no se ofrece.
 */
final class DomainCatalogs {

    static final Map<Domain, List<String>> FILTER_ATTRIBUTES = Map.of(
            Domain.MOVIMIENTO_EN_MASA, List.of("TIPO", "SUBTIPO", "CLAS_MAPA"),
            Domain.FALLA_GEOLOGICA, List.of("NombreFalla", "Tipo"),
            Domain.UNIDAD_GEOLOGICA, List.of("SimboloUC", "Edad"),
            Domain.DOMINIO_TECTONICO, List.of("NombreDT"),
            Domain.VOLCAN, List.of("NombreVolcan")
    );

    static final Map<Domain, List<String>> REQUIRED_FIELDS = Map.of(
            Domain.MOVIMIENTO_EN_MASA, List.of("TIPO"),
            Domain.FALLA_GEOLOGICA, List.of("NombreFalla"),
            Domain.UNIDAD_GEOLOGICA, List.of("SimboloUC"),
            Domain.DOMINIO_TECTONICO, List.of("NombreDT"),
            Domain.VOLCAN, List.of("NombreVolcan")
    );

    static final Map<Domain, List<String>> EDITABLE_ATTRIBUTES = Map.of(
            Domain.MOVIMIENTO_EN_MASA, List.of("ID", "INV_MOVIMI", "TIPO", "SUBTIPO", "CLAS_MAPA", "ETIQUETA_M"),
            Domain.FALLA_GEOLOGICA, List.of("Tipo", "NombreFalla", "Comentarios"),
            Domain.UNIDAD_GEOLOGICA, List.of("SimboloUC", "N°CartaColores", "Descripcion", "Edad", "UGIntegradas", "Comentarios"),
            Domain.DOMINIO_TECTONICO, List.of("CodigoDT", "NombreDT", "Label"),
            Domain.VOLCAN, List.of("VolcanID", "NombreVolcan", "AlturaSobreNivelMar", "Comentarios", "URL")
    );

    private DomainCatalogs() {
    }
}
