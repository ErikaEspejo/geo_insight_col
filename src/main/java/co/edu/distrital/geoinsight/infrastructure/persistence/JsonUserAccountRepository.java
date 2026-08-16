package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.domain.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistencia de cuentas sobre data/users.json (FR-024) con escritura atómica.
 */
@Repository
public class JsonUserAccountRepository implements UserAccountRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonUserAccountRepository.class);

    private final Path file;
    private final ObjectMapper objectMapper;
    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

    public JsonUserAccountRepository(Path dataDir, ObjectMapper objectMapper) {
        this.file = dataDir.resolve("users.json");
        this.objectMapper = objectMapper;
        load();
    }

    private void load() {
        accounts.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root.isArray()) {
                for (JsonNode node : root) {
                    UserAccount account = new UserAccount(
                            node.path("username").asText(),
                            node.path("passwordHash").asText(),
                            Role.valueOf(node.path("role").asText()));
                    accounts.put(account.username(), account);
                }
            }
        } catch (IOException e) {
            log.error("No se pudo leer {}: {}", file, e.getMessage());
        }
    }

    private void persist() {
        try {
            Files.createDirectories(file.getParent());
            ArrayNode array = objectMapper.createArrayNode();
            for (UserAccount account : accounts.values()) {
                array.add(objectMapper.createObjectNode()
                        .put("username", account.username())
                        .put("passwordHash", account.passwordHash())
                        .put("role", account.role().name()));
            }
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            objectMapper.writer().writeValue(temp.toFile(), array);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo persistir " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(accounts.get(username));
    }

    @Override
    public boolean existsByUsername(String username) {
        return accounts.containsKey(username);
    }

    @Override
    public List<UserAccount> findAll() {
        return List.copyOf(accounts.values());
    }

    @Override
    public UserAccount save(UserAccount account) {
        accounts.put(account.username(), account);
        persist();
        return account;
    }
}
