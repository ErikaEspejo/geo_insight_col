package co.edu.distrital.geoinsight.infrastructure.bootstrap;

import co.edu.distrital.geoinsight.infrastructure.persistence.AdminAccountSeeder;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * Orquestación del arranque: siembra del admin y verificación/descarga de
 * datasets. El bootstrap es infraestructura, no lógica de dominio.
 */
@Component
public class BootstrapRunner implements SmartInitializingSingleton {

    private final AdminAccountSeeder adminAccountSeeder;
    private final DatasetBootstrapService datasetBootstrapService;

    public BootstrapRunner(AdminAccountSeeder adminAccountSeeder, DatasetBootstrapService datasetBootstrapService) {
        this.adminAccountSeeder = adminAccountSeeder;
        this.datasetBootstrapService = datasetBootstrapService;
    }

    @Override
    public void afterSingletonsInstantiated() {
        adminAccountSeeder.seed();
        datasetBootstrapService.run();
    }
}
