package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.domain.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Siembra la cuenta administrador desde config/admin-account.json (FR-023).
 * La cuenta admin preexiste por configuración y nunca se crea por registro.
 */
@Service
public class AdminAccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final Path adminAccountFile;
    private final ObjectMapper objectMapper;

    public AdminAccountSeeder(UserAccountRepository userAccountRepository, Path adminAccountFile, ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.adminAccountFile = adminAccountFile;
        this.objectMapper = objectMapper;
    }

    public void seed() {
        if (!Files.exists(adminAccountFile)) {
            log.warn("No existe {}; no se siembra cuenta administrador", adminAccountFile);
            return;
        }
        try {
            JsonNode config = objectMapper.readTree(adminAccountFile.toFile());
            String username = config.path("username").asText();
            String passwordHash = config.path("passwordHash").asText();
            if (username.isBlank() || passwordHash.isBlank()) {
                log.warn("Cuenta admin mal configurada en {}", adminAccountFile);
                return;
            }
            if (!userAccountRepository.existsByUsername(username)) {
                userAccountRepository.save(new UserAccount(username, passwordHash, Role.ADMIN));
                log.info("Cuenta administrador sembrada: {}", username);
            }
        } catch (IOException e) {
            log.error("No se pudo leer la cuenta admin: {}", e.getMessage());
        }
    }
}
