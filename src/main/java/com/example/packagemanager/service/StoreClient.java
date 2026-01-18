package com.example.packagemanager.service;

import com.example.packagemanager.util.ChecksumUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Thin HTTP client that talks to the (mock) store.
 */
public final class StoreClient {

        /* ------------------------------------------------------------------ */
        private static final Logger log = LoggerFactory.getLogger(StoreClient.class);

        /** single, re-usable HTTP client – 10 s connect timeout */
        private static final HttpClient http = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();

        /*
         * ------------------------------------------------------------------ *
         * JSON → POJO representing one line of /catalog
         * ------------------------------------------------------------------
         */
        public record CatalogEntry(String packageName,
                        String sha256,
                        String version) {
        }

        /*
         * ------------------------------------------------------------------ *
         * 1 · GET /catalog
         * ------------------------------------------------------------------
         */
        public static Map<String, CatalogEntry> fetchCatalog(String baseUrl) throws Exception {

                HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/catalog"))
                                .timeout(Duration.ofSeconds(10))
                                .GET()
                                .build();

                String json = http.send(req, HttpResponse.BodyHandlers.ofString()).body();

                ObjectMapper mapper = new ObjectMapper();
                List<CatalogEntry> list = mapper.readValue(json,
                                new TypeReference<List<CatalogEntry>>() {
                                });

                return list.stream()
                                .collect(Collectors.toMap(CatalogEntry::packageName, e -> e));
        }

        /*
         * ------------------------------------------------------------------ *
         * 2 · GET /packages/<zip> – download
         * ------------------------------------------------------------------
         */
        public static Path downloadPackage(String baseUrl,
                        String packageName,
                        Path localPath) throws Exception {

                HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/packages/" + packageName))
                                .timeout(Duration.ofMinutes(1))
                                .GET()
                                .build();

                HttpResponse<InputStream> resp = http.send(
                                req, HttpResponse.BodyHandlers.ofInputStream());

                if (resp.statusCode() != 200)
                        throw new IllegalStateException("Download failed: HTTP " + resp.statusCode());

                Files.createDirectories(localPath.getParent());
                Files.copy(resp.body(), localPath, StandardCopyOption.REPLACE_EXISTING);

                log.debug("Saved {} ({} bytes, sha={})",
                                localPath,
                                Files.size(localPath),
                                ChecksumUtil.sha256(localPath));

                return localPath;
        }

        /*
         * ------------------------------------------------------------------ *
         * 3 · POST /upload – raw body
         * ------------------------------------------------------------------
         */
        public static void uploadPackage(String baseUrl, Path localZip) throws Exception {

                HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + "/upload"))
                                .timeout(Duration.ofMinutes(1))
                                .header("Content-Type", "application/zip")
                                .header("X-Filename", localZip.getFileName().toString())
                                .POST(HttpRequest.BodyPublishers.ofFile(localZip))
                                .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() != 200)
                        throw new IllegalStateException("Upload failed: HTTP " + resp.statusCode());
        }

        private StoreClient() {
        } // utility class
}
