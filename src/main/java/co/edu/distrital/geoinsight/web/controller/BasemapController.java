package co.edu.distrital.geoinsight.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Fondo base de Colombia servido desde el classpath para visualización offline
 * (research.md §3.3).
 */
@RestController
@RequestMapping("/api/basemap")
public class BasemapController {

    private static final String COLOMBIA_GEOJSON = "basemap/colombia.geojson";

    private final ObjectMapper objectMapper;
    private volatile JsonNode cachedBasemap;

    public BasemapController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/colombia", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode colombia() {
        if (cachedBasemap == null) {
            cachedBasemap = loadBasemap();
        }
        return cachedBasemap;
    }

    private JsonNode loadBasemap() {
        try (InputStream input = new ClassPathResource(COLOMBIA_GEOJSON).getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar el fondo de Colombia", e);
        }
    }
}
