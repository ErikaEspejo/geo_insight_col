package co.edu.distrital.geoinsight.web;

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
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PerformanceAcceptanceTest {

    private static final Duration MAP_API_BUDGET = Duration.ofSeconds(10);
    private static final Duration ANALYSIS_API_BUDGET = Duration.ofSeconds(5);

    @Autowired
    MockMvc mockMvc;

    static String tempDataDir;

    static {
        try {
            tempDataDir = Files.createTempDirectory("geo-insight-performance-test-data").toString();
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
    void heavyLayerResponseMeetsServerBudget() throws Exception {
        long started = System.nanoTime();
        MvcResult result = mockMvc.perform(get("/api/layers/UNIDAD_GEOLOGICA/geojson").session(session))
                .andExpect(status().isOk())
                .andReturn();
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        report("SC-001 API capa de unidades", elapsed);
        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
        assertThat(elapsed).isLessThan(MAP_API_BUDGET);
    }

    @Test
    void coordinateContextMeetsBudget() throws Exception {
        Duration elapsed = timedPost("/api/context", "{\"lon\":-76.166495,\"lat\":2.232286}");
        report("SC-002 contexto", elapsed);
        assertThat(elapsed).isLessThan(ANALYSIS_API_BUDGET);
    }

    @Test
    void zoneAnalysisMeetsBudget() throws Exception {
        Duration elapsed = timedPost("/api/zones/analyze",
                "{\"lon\":-76.166495,\"lat\":2.232286,\"radiusMeters\":50000}");
        report("SC-003 zona", elapsed);
        assertThat(elapsed).isLessThan(ANALYSIS_API_BUDGET);
    }

    private Duration timedPost(String path, String body) throws Exception {
        long started = System.nanoTime();
        mockMvc.perform(post(path).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        return Duration.ofNanos(System.nanoTime() - started);
    }

    private void report(String scenario, Duration elapsed) {
        System.out.printf("PERFORMANCE %s: %.3f s%n", scenario, elapsed.toNanos() / 1_000_000_000.0);
    }
}
