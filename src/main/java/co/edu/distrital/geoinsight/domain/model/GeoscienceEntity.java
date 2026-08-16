package co.edu.distrital.geoinsight.domain.model;

import co.edu.distrital.geoinsight.domain.geometry.Geometry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Entidad geocientífica común de los cinco dominios. Inmutable: origen,
 * dominio e id se fijan al crearla. Los atributos preservan las claves reales
 * del dataset (FR-008, FR-018).
 */
public final class GeoscienceEntity {

    private final String id;
    private final Domain domain;
    private final Origin origin;
    private final Geometry geometry;
    private final Map<String, Object> attributes;

    public GeoscienceEntity(String id, Domain domain, Origin origin, Geometry geometry, Map<String, Object> attributes) {
        this.id = Objects.requireNonNull(id, "id requerido");
        this.domain = Objects.requireNonNull(domain, "dominio requerido");
        this.origin = Objects.requireNonNull(origin, "origen requerido");
        this.geometry = Objects.requireNonNull(geometry, "geometría requerida");
        this.attributes = attributes == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String id() {
        return id;
    }

    public Domain domain() {
        return domain;
    }

    public Origin origin() {
        return origin;
    }

    public Geometry geometry() {
        return geometry;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    public Object attribute(String name) {
        return attributes.get(name);
    }

    public Optional<String> attributeString(String name) {
        Object value = attributes.get(name);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(value));
    }
}
