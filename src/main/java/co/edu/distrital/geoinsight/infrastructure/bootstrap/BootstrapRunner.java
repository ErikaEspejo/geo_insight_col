package co.edu.distrital.geoinsight.infrastructure.bootstrap;

import co.edu.distrital.geoinsight.infrastructure.persistence.AdminAccountSeeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Orquestación del arranque: siembra del admin y verificación/descarga de
 * datasets. El bootstrap es infraestructura, no lógica de dominio.
 */
@Component
public class BootstrapRunner implements ApplicationRunner {

    private final AdminAccountSeeder adminAccountSeeder;
    private final DatasetBootstrapService datasetBootstrapService;

    public BootstrapRunner(AdminAccountSeeder adminAccountSeeder, DatasetBootstrapService datasetBootstrapService) {
        this.adminAccountSeeder = adminAccountSeeder;
        this.datasetBootstrapService = datasetBootstrapService;
    }

    @Override
    public void run(ApplicationArguments args) {
        adminAccountSeeder.seed();
        datasetBootstrapService.run();
    }
}
