package co.edu.distrital.geoinsight.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LayerWebTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    static String tempDataDir;

    static {
        try {
            tempDataDir = Files.createTempDirectory("geo-insight-layer-test-data").toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("geoinsight.data-dir", () -> tempDataDir);
    }

    private MockHttpSession session;

    @BeforeEach
    void login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void unauthenticatedLayersAreRejected() throws Exception {
        mockMvc.perform(get("/api/layers")).andExpect(status().isUnauthorized());
    }

    @Test
    void basemapIsPublic() throws Exception {
        mockMvc.perform(get("/api/basemap/colombia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }

    @Test
    void layersReturnsFiveDomains() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/layers").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode layers = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(layers).hasSize(5);
        assertThat(layers.path(0).path("geometryType").asText()).isIn("Point", "LineString", "Polygon");
        assertThat(layers.path(0).path("count").asInt()).isPositive();
        assertThat(layers.path(0).path("filterableAttributes").isArray()).isTrue();
        assertThat(layers.path(0).path("editableAttributes").isArray()).isTrue();
        for (JsonNode layer : layers) {
            assertThat(layer.path("dataAvailable").asBoolean())
                    .as("dataset cargado para %s", layer.path("domain").asText()).isTrue();
        }
    }

    @Test
    void volcanoEntitiesAreGeoJsonPoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/layers/VOLCAN/geojson").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andReturn();
        JsonNode collection = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(collection.path("features")).hasSize(61);
        assertThat(collection.path("features").path(0).path("geometry").path("type").asText()).isEqualTo("Point");
        assertThat(collection.path("features").path(0).path("properties").path("origin").asText()).isEqualTo("SGC");
    }

    @Test
    void filteredEntitiesMatchRealAttributeValues() throws Exception {
        MvcResult layersResult = mockMvc.perform(get("/api/layers").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode layers = objectMapper.readTree(layersResult.getResponse().getContentAsString());
        JsonNode volcano = findDomain(layers, "VOLCAN");
        JsonNode attr = volcano.path("filterableAttributes").path(0);
        String attribute = attr.path("name").asText();
        String value = attr.path("values").path(0).asText();

        MvcResult result = mockMvc.perform(get("/api/entities/VOLCAN")
                        .queryParam(attribute, value).session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode collection = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(collection.path("features")).isNotEmpty();
        for (JsonNode feature : collection.path("features")) {
            assertThat(feature.path("properties").path(attribute).asText()).isEqualTo(value);
        }
    }

    @Test
    void filterWithUnknownAttributeIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/entities/VOLCAN")
                        .queryParam("AtributoInexistente", "x").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDomainIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/entities/NO_EXISTE").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void entityDetailIncludesOrigin() throws Exception {
        MvcResult listResult = mockMvc.perform(get("/api/layers/VOLCAN/geojson").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode collection = objectMapper.readTree(listResult.getResponse().getContentAsString());
        String id = collection.path("features").path(0).path("id").asText();

        mockMvc.perform(get("/api/entities/VOLCAN/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.origin").value("SGC"))
                .andExpect(jsonPath("$.attributes").exists());
    }

    @Test
    void heavyPolygonLayerIsSimplifiedForVisualization() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/entities/UNIDAD_GEOLOGICA").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andReturn();
        JsonNode collection = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(collection.path("features")).hasSize(7461);
        JsonNode geometry = collection.path("features").path(0).path("geometry");
        assertThat(geometry.path("type").asText()).isIn("Polygon", "MultiPolygon");
        assertThat(hasCoordinateBeyondFourDecimals(geometry)).as("coordenadas simplificadas a <=4 decimales").isFalse();
    }

    private boolean hasCoordinateBeyondFourDecimals(JsonNode geometry) {
        String type = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");
        return switch (type) {
            case "Polygon" -> coordinatesBeyondFourDecimals(coordinates.path(0));
            case "MultiPolygon" -> {
                boolean beyond = false;
                for (JsonNode polygon : coordinates) {
                    beyond |= coordinatesBeyondFourDecimals(polygon.path(0));
                }
                yield beyond;
            }
            default -> false;
        };
    }

    private boolean coordinatesBeyondFourDecimals(JsonNode ring) {
        for (JsonNode coordinate : ring) {
            double lon = coordinate.path(0).asDouble();
            double lat = coordinate.path(1).asDouble();
            if (Math.abs(lon * 10000.0 - Math.round(lon * 10000.0)) > 1e-6
                    || Math.abs(lat * 10000.0 - Math.round(lat * 10000.0)) > 1e-6) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findDomain(JsonNode layers, String domain) {
        for (JsonNode layer : layers) {
            if (layer.path("domain").asText().equals(domain)) {
                return layer;
            }
        }
        throw new AssertionError("Dominio no encontrado: " + domain);
    }
}
