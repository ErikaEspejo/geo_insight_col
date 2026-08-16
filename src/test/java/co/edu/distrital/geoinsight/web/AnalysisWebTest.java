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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisWebTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    static String tempDataDir;

    static {
        try {
            tempDataDir = Files.createTempDirectory("geo-insight-analysis-test-data").toString();
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
    void contextReturnsExplicitAbsenceWhenOutOfCoverage() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/context").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lon\":-55.0,\"lat\":-5.0}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("coordinate").path("lon").asDouble()).isEqualTo(-55.0);
        assertThat(body.path("geologicalUnits")).isEmpty();
        assertThat(body.path("tectonicDomains")).isEmpty();
        assertThat(body.path("nearestFault").path("distanceMeters").asDouble()).isGreaterThan(1_000_000);
        assertThat(body.path("nearestMassMovement").path("distanceMeters").asDouble()).isGreaterThan(1_000_000);
        assertThat(body.path("nearestVolcano").path("distanceMeters").asDouble()).isGreaterThan(1_000_000);
    }

    @Test
    void contextInsideCoverageReturnsNearestWithDistance() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/context").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lon\":-76.166495,\"lat\":2.232286}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("geologicalUnits").path(0).path("origin").asText()).isEqualTo("SGC");
        assertThat(body.path("nearestVolcano").path("entity").path("attributes").path("NombreVolcan").asText())
                .isEqualTo("Volcan Santa Leticia");
        assertThat(body.path("nearestVolcano").path("distanceMeters").asDouble()).isLessThan(100.0);
    }

    @Test
    void invalidCoordinateIsRejected() throws Exception {
        mockMvc.perform(post("/api/context").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lon\":-200.0,\"lat\":2.0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/context").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lat\":2.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zoneAnalysisReturnsBreakdownsWithoutRiskLanguage() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/zones/analyze").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lon\":-76.166495,\"lat\":2.232286,\"radiusMeters\":50000}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("zone").path("radiusMeters").asDouble()).isEqualTo(50000.0);
        assertThat(body.path("massMovements").path("count").isNumber()).isTrue();
        assertThat(body.path("massMovements").path("dataAvailable").asBoolean()).isTrue();
        assertThat(body.path("massMovements").path("byTipo").isObject()).isTrue();
        assertThat(body.path("volcanoes").path("count").asInt()).isGreaterThanOrEqualTo(1);
        String raw = result.getResponse().getContentAsString().toLowerCase();
        for (String forbidden : new String[]{"riesgo", "amenaza", "peligrosidad", "predicci"}) {
            assertThat(raw).doesNotContain(forbidden);
        }
    }

    @Test
    void invalidRadiusIsRejected() throws Exception {
        mockMvc.perform(post("/api/zones/analyze").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lon\":-76.0,\"lat\":2.0,\"radiusMeters\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zoneComparisonShowsSameIndicatorsSideBySide() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/zones/compare").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneA\":{\"lon\":-76.166495,\"lat\":2.232286,\"radiusMeters\":30000},"
                                + "\"zoneB\":{\"lon\":-74.07,\"lat\":4.71,\"radiusMeters\":30000}}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("zoneA").path("massMovements").has("count")).isTrue();
        assertThat(body.path("zoneB").path("massMovements").has("count")).isTrue();
        assertThat(body.path("zoneA").path("nearestMassMovement").path("distanceMeters").isNumber()).isTrue();
        assertThat(body.path("zoneB").path("nearestFault").path("distanceMeters").isNumber()).isTrue();
        assertThat(body.path("zoneA").path("nearestVolcano").path("entity").path("attributes")
                .has("NombreVolcan")).isTrue();
        assertThat(body.path("zoneA").path("centerGeologicalUnits").isArray()).isTrue();
        assertThat(body.path("zoneB").path("centerTectonicDomains").isArray()).isTrue();
        assertThat(body.path("zoneA").path("faults").path("dataAvailable").asBoolean()).isTrue();
        List<String> zoneAFields = fieldNames(body.path("zoneA"));
        List<String> zoneBFields = fieldNames(body.path("zoneB"));
        assertThat(zoneAFields).containsExactlyElementsOf(zoneBFields);
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
