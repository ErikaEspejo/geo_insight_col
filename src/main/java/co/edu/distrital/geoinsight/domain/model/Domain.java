package co.edu.distrital.geoinsight.domain.model;

import java.util.Optional;

/**
 * Los cinco dominios geocientíficos integrados. La geometría admitida es la
 * declarada en el contexto funcional (research.md §2).
 */
public enum Domain {

    MOVIMIENTO_EN_MASA("Movimientos en masa", GeometryKind.POINT),
    FALLA_GEOLOGICA("Fallas geológicas", GeometryKind.LINE),
    UNIDAD_GEOLOGICA("Unidades geológicas", GeometryKind.POLYGON),
    DOMINIO_TECTONICO("Dominios tectónicos", GeometryKind.POLYGON),
    VOLCAN("Volcanes", GeometryKind.POINT);

    private final String displayName;
    private final GeometryKind geometryKind;

    Domain(String displayName, GeometryKind geometryKind) {
        this.displayName = displayName;
        this.geometryKind = geometryKind;
    }

    public String displayName() {
        return displayName;
    }

    public GeometryKind geometryKind() {
        return geometryKind;
    }

    public static Optional<Domain> fromKey(String key) {
        for (Domain domain : values()) {
            if (domain.name().equals(key)) {
                return Optional.of(domain);
            }
        }
        return Optional.empty();
    }
}
