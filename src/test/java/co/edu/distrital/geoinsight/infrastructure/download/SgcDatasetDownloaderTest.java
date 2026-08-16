package co.edu.distrital.geoinsight.infrastructure.download;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.infrastructure.persistence.SgcDatasets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SgcDatasetDownloaderTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void stopServer() throws IOException {
        server.stop(0);
    }

    @Test
    void paginatesAndVerifiesExpectedCount() throws IOException {
        AtomicInteger countRequests = new AtomicInteger();
        server.createContext("/svc/0/query", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String body;
            if (query != null && query.contains("returnCountOnly=true")) {
                countRequests.incrementAndGet();
                body = "{\"count\": 3}";
            } else if (query != null && query.contains("resultOffset=0")) {
                body = "{\"features\":[{\"f1\":1},{\"f2\":2}]}";
            } else {
                body = "{\"features\":[{\"f3\":3}]}";
            }
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        String serviceUrl = "http://localhost:" + server.getAddress().getPort() + "/svc";
        SgcDatasets.Source source = new SgcDatasets.Source("test.geojson", serviceUrl, 0, 3, 2, Domain.VOLCAN);
        HttpClient client = HttpClient.newHttpClient();
        SgcDatasetDownloader downloader = new SgcDatasetDownloader(tempDir, new ObjectMapper(), client);

        downloader.download(source);

        assertThat(countRequests.get()).isEqualTo(1);
        Path file = tempDir.resolve("test.geojson");
        assertThat(file).exists();
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readTree(file.toFile()).path("features")).hasSize(3);
    }

    @Test
    void retriesAfterTransientHttpFailure() throws IOException {
        AtomicInteger countRequests = new AtomicInteger();
        server.createContext("/svc/0/query", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            int status = 200;
            String body;
            if (query != null && query.contains("returnCountOnly=true")) {
                if (countRequests.incrementAndGet() == 1) {
                    status = 503;
                    body = "{}";
                } else {
                    body = "{\"count\":1}";
                }
            } else {
                body = "{\"features\":[{\"f1\":1}]}";
            }
            byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();

        String serviceUrl = "http://localhost:" + server.getAddress().getPort() + "/svc";
        SgcDatasets.Source source = new SgcDatasets.Source("retry.geojson", serviceUrl, 0, 1, 1, Domain.VOLCAN);
        SgcDatasetDownloader downloader = new SgcDatasetDownloader(
                tempDir, new ObjectMapper(), HttpClient.newHttpClient(), 3, 0);

        downloader.download(source);

        assertThat(countRequests.get()).isEqualTo(2);
        assertThat(tempDir.resolve("retry.geojson")).exists();
    }
}
