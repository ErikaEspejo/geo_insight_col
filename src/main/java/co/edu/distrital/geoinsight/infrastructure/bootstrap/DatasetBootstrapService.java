package co.edu.distrital.geoinsight.infrastructure.bootstrap;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.infrastructure.download.SgcDatasetDownloader;
import co.edu.distrital.geoinsight.infrastructure.persistence.GeoJsonDatasetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Verifica que los cinco datasets existan y estén completos; descarga los que
 * falten (bootstrap de infraestructura, nunca lógica de dominio). Si la red no
 * está disponible, deja el indicador de datos ausentes (FR-020).
 */
@Service
public class DatasetBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(DatasetBootstrapService.class);

    private final GeoJsonDatasetRepository datasetRepository;
    private final SgcDatasetDownloader downloader;

    public DatasetBootstrapService(GeoJsonDatasetRepository datasetRepository, SgcDatasetDownloader downloader) {
        this.datasetRepository = datasetRepository;
        this.downloader = downloader;
    }

    public Set<Domain> run() {
        Set<Domain> missing = datasetRepository.missingDatasets();
        if (!missing.isEmpty()) {
            log.info("Datasets faltantes: {}. Intentando descarga...", missing);
            downloader.downloadMissing(missing);
            datasetRepository.loadAll();
        }
        Set<Domain> stillMissing = datasetRepository.missingDatasets();
        if (!stillMissing.isEmpty()) {
            log.warn("El sistema inicia con datos ausentes para: {}", stillMissing);
        }
        return stillMissing;
    }
}
