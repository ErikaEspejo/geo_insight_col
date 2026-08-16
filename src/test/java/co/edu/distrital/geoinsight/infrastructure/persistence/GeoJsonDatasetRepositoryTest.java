package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GeoJsonDatasetRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsRealSgcDatasetsWithExpectedCounts() {
        Path datasetsDir = Path.of("docs/datasets").toAbsolutePath();
        assumeTrue(Files.isDirectory(datasetsDir), "datasets no presentes; se omite la prueba");

        GeoJsonDatasetRepository repository = new GeoJsonDatasetRepository(datasetsDir, new ObjectMapper());
        repository.loadAll();

        assertThat(repository.missingDatasets()).isEmpty();
        assertThat(repository.findSgcByDomain(Domain.VOLCAN)).hasSize(61);
        assertThat(repository.findSgcByDomain(Domain.FALLA_GEOLOGICA)).hasSize(4866);
        assertThat(repository.findSgcByDomain(Domain.UNIDAD_GEOLOGICA)).hasSize(7461);
        assertThat(repository.findSgcByDomain(Domain.DOMINIO_TECTONICO)).hasSize(3);
        assertThat(repository.findSgcByDomain(Domain.MOVIMIENTO_EN_MASA)).hasSize(6826);

        assertThat(repository.attributeNames(Domain.MOVIMIENTO_EN_MASA)).contains("TIPO", "SUBTIPO", "CLAS_MAPA");
        assertThat(repository.distinctValues(Domain.MOVIMIENTO_EN_MASA, "TIPO")).isNotEmpty();
        assertThat(repository.editableAttributeTypes(Domain.MOVIMIENTO_EN_MASA))
                .containsEntry("ID", AttributeValueType.INTEGER)
                .containsEntry("TIPO", AttributeValueType.TEXT);
        assertThat(repository.editableAttributeTypes(Domain.VOLCAN))
                .containsEntry("AlturaSobreNivelMar", AttributeValueType.INTEGER);
    }

    @Test
    void missingFileMarksDomainAsMissing() {
        GeoJsonDatasetRepository repository = new GeoJsonDatasetRepository(tempDir, new ObjectMapper());
        repository.loadAll();
        assertThat(repository.missingDatasets()).containsAll(java.util.Set.copyOf(java.util.List.of(Domain.values())));
    }

    @Test
    void sgcEntitiesAreReadOnlyOrigin() {
        Path datasetsDir = Path.of("docs/datasets").toAbsolutePath();
        assumeTrue(Files.isDirectory(datasetsDir), "datasets no presentes; se omite la prueba");

        GeoJsonDatasetRepository repository = new GeoJsonDatasetRepository(datasetsDir, new ObjectMapper());
        repository.loadAll();
        GeoscienceEntity fault = repository.findSgcByDomain(Domain.FALLA_GEOLOGICA).get(0);
        assertThat(fault.origin().name()).isEqualTo("SGC");
        assertThat(fault.id()).startsWith("SGC-");
    }
}
