package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.geometry.Coordinate;
import co.edu.distrital.geoinsight.domain.geometry.GeometryFactory;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.model.Origin;
import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRepositoriesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void userAccountSurvivesRepositoryReload() throws Exception {
        JsonUserAccountRepository writer = new JsonUserAccountRepository(tempDir, objectMapper);
        writer.save(new UserAccount("ana", "$2a$10$hash", Role.USER));

        JsonUserAccountRepository reader = new JsonUserAccountRepository(tempDir, objectMapper);

        assertThat(reader.findByUsername("ana")).get()
                .extracting(UserAccount::passwordHash, UserAccount::role)
                .containsExactly("$2a$10$hash", Role.USER);
        assertThat(Files.exists(tempDir.resolve("users.json.tmp"))).isFalse();
    }

    @Test
    void geoEntitySurvivesUpdateDeleteAndRepositoryReload() throws Exception {
        JsonGeoEntityRepository writer = new JsonGeoEntityRepository(tempDir, objectMapper);
        GeoscienceEntity original = entity("Original");
        writer.save(original);
        writer.save(entity("Actualizado"));

        JsonGeoEntityRepository reader = new JsonGeoEntityRepository(tempDir, objectMapper);
        assertThat(reader.findById("GEO-1")).get()
                .extracting(found -> found.attributes().get("NombreVolcan"))
                .isEqualTo("Actualizado");

        reader.delete("GEO-1");
        JsonGeoEntityRepository afterDelete = new JsonGeoEntityRepository(tempDir, objectMapper);
        assertThat(afterDelete.findById("GEO-1")).isEmpty();
        assertThat(Files.exists(tempDir.resolve("geoentities.json.tmp"))).isFalse();
    }

    private GeoscienceEntity entity(String name) {
        return new GeoscienceEntity("GEO-1", Domain.VOLCAN, Origin.GEOINSIGHT,
                GeometryFactory.point(new Coordinate(-74.0, 4.7)),
                Map.of("NombreVolcan", name, "AlturaSobreNivelMar", 4300L));
    }
}
