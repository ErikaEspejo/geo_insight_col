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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthWebTest {

    @Autowired
    MockMvc mockMvc;

    static String tempDataDir;

    static {
        try {
            tempDataDir = Files.createTempDirectory("geo-insight-test-data").toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("geoinsight.data-dir", () -> tempDataDir);
    }

    @BeforeEach
    void ensureSessionIsFresh() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/layers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void browserNavigationWithoutSessionRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/").header("Accept", "text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));
    }

    @Test
    void loginVisualAssetsArePublicWithoutSession() throws Exception {
        mockMvc.perform(get("/css/login.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/js/auth.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/images/geoinsight-logo.png"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/images/colombian-volcanic-landscape.png"))
                .andExpect(status().isOk());
    }

    @Test
    void applicationAssetsRequireSession() throws Exception {
        mockMvc.perform(get("/js/admin.js"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/css/layers.css"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/lib/leaflet/leaflet.js"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndMeFlow() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.admin").value(false));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave123\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"clave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.admin").value(false))
                .andReturn();

        mockMvc.perform(get("/api/auth/me").session((MockHttpSession) login.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana"));
    }

    @Test
    void loginRotatesAnExistingSessionIdentifier() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"beatriz\",\"password\":\"clave123\"}"))
                .andExpect(status().isCreated());
        MockHttpSession existingSession = new MockHttpSession();
        String previousId = existingSession.getId();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .session(existingSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"beatriz\",\"password\":\"clave123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(previousId);
    }

    @Test
    void loginWithWrongCredentialsIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ana\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void seededAdminLogsInAsAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admin").value(true))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
