package co.edu.distrital.geoinsight.infrastructure.persistence;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;
import co.edu.distrital.geoinsight.domain.repository.GeoEntityRepository;
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
 * Persistencia de entidades GEOINSIGHT sobre data/geoentities.json con
 * escritura atómica (FR-005, FR-019).
 */
@Repository
public class JsonGeoEntityRepository implements GeoEntityRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonGeoEntityRepository.class);

    private final Path file;
    private final ObjectMapper objectMapper;
    private final Map<String, GeoscienceEntity> entities = new LinkedHashMap<>();

    public JsonGeoEntityRepository(Path dataDir, ObjectMapper objectMapper) {
        this.file = dataDir.resolve("geoentities.json");
        this.objectMapper = objectMapper;
        load();
    }

    private void load() {
        entities.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root.isArray()) {
                for (JsonNode node : root) {
                    GeoscienceEntity entity = DomainJsonCodec.entityFromJson(objectMapper, node);
                    entities.put(entity.id(), entity);
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
            entities.values().forEach(entity -> array.add(DomainJsonCodec.entityToJson(objectMapper, entity)));
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            objectMapper.writer().writeValue(temp.toFile(), array);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo persistir " + file + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<GeoscienceEntity> findAll() {
        return List.copyOf(entities.values());
    }

    @Override
    public List<GeoscienceEntity> findByDomain(Domain domain) {
        return entities.values().stream()
                .filter(entity -> entity.domain() == domain)
                .toList();
    }

    @Override
    public Optional<GeoscienceEntity> findById(String id) {
        return Optional.ofNullable(entities.get(id));
    }

    @Override
    public GeoscienceEntity save(GeoscienceEntity entity) {
        entities.put(entity.id(), entity);
        persist();
        return entity;
    }

    @Override
    public void delete(String id) {
        entities.remove(id);
        persist();
    }
}
