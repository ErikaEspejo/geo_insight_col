package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.repository.CountryBoundary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GeoJsonCountryBoundary implements CountryBoundary {
    private static final String COLOMBIA_GEOJSON = "basemap/colombia.geojson";
    private final List<Geometry> geometries;

    public GeoJsonCountryBoundary(ObjectMapper objectMapper) {
        this.geometries = load(objectMapper);
    }

    @Override
    public boolean contains(Coordinate coordinate) {
        return geometries.stream().anyMatch(geometry -> geometry.contains(coordinate));
    }

    private List<Geometry> load(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(COLOMBIA_GEOJSON).getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            List<Geometry> result = new ArrayList<>();
            for (JsonNode feature : root.path("features")) {
                result.add(DomainJsonCodec.geometryFromJson(feature.path("geometry")));
            }
            return List.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar el límite de Colombia", e);
        }
    }
}
