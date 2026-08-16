package co.edu.distrital.geoinsight.infrastructure.download;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.infrastructure.persistence.SgcDatasets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Descarga los datasets SGC desde las APIs REST oficiales (ArcGIS Feature
 * Server) replicando la lógica verificada de scripts/download-datasets.ps1:
 * conteo total, paginación y verificación exacta (research.md §1).
 */
public class SgcDatasetDownloader {

    private static final Logger log = LoggerFactory.getLogger(SgcDatasetDownloader.class);

    private final Path datasetsDir;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SgcDatasetDownloader(Path datasetsDir, ObjectMapper objectMapper) {
        this(datasetsDir, objectMapper, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build());
    }

    SgcDatasetDownloader(Path datasetsDir, ObjectMapper objectMapper, HttpClient httpClient) {
        this.datasetsDir = datasetsDir;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /** Descarga todos los datasets ausentes. Retorna los dominios que quedaron sin datos. */
    public Set<Domain> downloadMissing(Set<Domain> missing) {
        for (SgcDatasets.Source source : SgcDatasets.SOURCES) {
            if (missing.contains(source.domain())) {
                download(source);
            }
        }
        return missing;
    }

    void download(SgcDatasets.Source source) {        try {
            long total = featureCount(source);
            List<JsonNode> features = new ArrayList<>();
            int offset = 0;
            while (offset < total) {
                List<JsonNode> page = fetchPage(source, offset);
                features.addAll(page);
                offset += source.pageSize();
            }
            if (features.size() != source.expectedCount()) {
                throw new IOException("Conteo inesperado para " + source.fileName() + ": " + features.size()
                        + " (esperado " + source.expectedCount() + ")");
            }
            writeFeatureCollection(features, datasetsDir.resolve(source.fileName()));
            log.info("Descargado y verificado {}: {} entidades", source.fileName(), features.size());
        } catch (IOException | InterruptedException e) {
            log.error("No se pudo descargar {}: {}", source.fileName(), e.getMessage());
        }
    }

    private long featureCount(SgcDatasets.Source source) throws IOException, InterruptedException {
        String url = source.serviceUrl() + "/" + source.layer()
                + "/query?f=json&where=" + encode("1=1") + "&returnCountOnly=true";
        JsonNode response = get(url);
        return response.path("count").asLong(0);
    }

    private List<JsonNode> fetchPage(SgcDatasets.Source source, int offset) throws IOException, InterruptedException {
        String url = source.serviceUrl() + "/" + source.layer()
                + "/query?f=geojson&where=" + encode("1=1")
                + "&outFields=*&outSR=4326"
                + "&resultOffset=" + offset + "&resultRecordCount=" + source.pageSize();
        JsonNode response = get(url);
        List<JsonNode> features = new ArrayList<>();
        JsonNode featuresNode = response.path("features");
        if (featuresNode.isArray()) {
            featuresNode.forEach(features::add);
        }
        return features;
    }

    private JsonNode get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("HTTP " + response.statusCode() + " al consultar " + url);
        }
        return objectMapper.readTree(response.body());
    }

    private void writeFeatureCollection(List<JsonNode> features, Path destination) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FeatureCollection");
        ArrayNode array = root.putArray("features");
        features.forEach(array::add);
        Files.createDirectories(destination.getParent());
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
        objectMapper.writer().writeValue(temp.toFile(), root);
        Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
