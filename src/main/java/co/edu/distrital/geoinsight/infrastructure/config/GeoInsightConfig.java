package co.edu.distrital.geoinsight.infrastructure.config;

import co.edu.distrital.geoinsight.infrastructure.download.SgcDatasetDownloader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class GeoInsightConfig {

    @Bean
    Path datasetsDir(@Value("${geoinsight.datasets-dir}") String datasetsDir) {
        return Paths.get(datasetsDir).toAbsolutePath();
    }

    @Bean
    Path dataDir(@Value("${geoinsight.data-dir}") String dataDir) {
        return Paths.get(dataDir).toAbsolutePath();
    }

    @Bean
    Path adminAccountFile(@Value("${geoinsight.admin-account-file}") String adminAccountFile) {
        return Paths.get(adminAccountFile).toAbsolutePath();
    }

    @Bean
    SgcDatasetDownloader sgcDatasetDownloader(Path datasetsDir, ObjectMapper objectMapper) {
        return new SgcDatasetDownloader(datasetsDir, objectMapper);
    }
}
