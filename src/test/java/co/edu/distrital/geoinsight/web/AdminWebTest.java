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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminWebTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    static String tempDataDir;

    static {
        try {
            tempDataDir = Files.createTempDirectory("geo-insight-admin-test-data").toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("geoinsight.data-dir", () -> tempDataDir);
    }

    private MockHttpSession adminSession;
    private MockHttpSession userSession;

    private static boolean consultaRegistered;

    @BeforeEach
    void login() throws Exception {
        MvcResult admin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        adminSession = (MockHttpSession) admin.getRequest().getSession(false);

        if (!consultaRegistered) {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"consulta\",\"password\":\"clave123\"}"))
                    .andExpect(status().isCreated());
            consultaRegistered = true;
        }
        MvcResult user = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"consulta\",\"password\":\"clave123\"}"))
                .andExpect(status().isOk()).andReturn();
        userSession = (MockHttpSession) user.getRequest().getSession(false);
    }

    @Test
    void userRoleCannotManageEntities() throws Exception {
        mockMvc.perform(post("/api/admin/entities").session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.7]},\"attributes\":{\"NombreVolcan\":\"Nuevo\"}}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesEditsAndDeletesGeoInsightEntity() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/admin/entities").session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.7]},\"attributes\":{\"NombreVolcan\":\"Volcán Nuevo\",\"AlturaSobreNivelMar\":4200}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("GEOINSIGHT"))
                .andExpect(jsonPath("$.domain").value("VOLCAN"))
                .andReturn();
        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = createdBody.path("id").asText();
        assertThat(id).startsWith("GEO-");

        mockMvc.perform(put("/api/admin/entities/" + id).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.8]},\"attributes\":{\"NombreVolcan\":\"Volcán Renombrado\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attributes.NombreVolcan").value("Volcán Renombrado"));

        mockMvc.perform(get("/api/admin/entities").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(delete("/api/admin/entities/" + id).session(adminSession))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/entities").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void missingRequiredFieldIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/entities").session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.7]},\"attributes\":{}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inventedAttributeIsRejected() throws Exception {
        mockMvc.perform(post("/api/admin/entities").session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.7]},\"attributes\":{\"NombreVolcan\":\"Nuevo\",\"CampoInventado\":\"x\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectedGeometryTypeIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/entities").session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-74.0,4.7],[-74.1,4.7],[-74.1,4.8],[-74.0,4.8],[-74.0,4.7]]]},\"attributes\":{\"NombreVolcan\":\"X\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sgcEntityCannotBeEditedOrDeleted() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/layers/VOLCAN/geojson").session(adminSession))
                .andExpect(status().isOk()).andReturn();
        JsonNode collection = objectMapper.readTree(list.getResponse().getContentAsString());
        String sgcId = collection.path("features").path(0).path("id").asText();

        mockMvc.perform(put("/api/admin/entities/" + sgcId).session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"VOLCAN\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[-74.0,4.7]},\"attributes\":{\"NombreVolcan\":\"Modificado\"}}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/entities/" + sgcId).session(adminSession))
                .andExpect(status().isForbidden());
    }
}
