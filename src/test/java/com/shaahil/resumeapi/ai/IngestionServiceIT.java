package com.shaahil.resumeapi.ai;

import com.shaahil.resumeapi.PostgresTestContainerConfig;
import com.shaahil.resumeapi.ai.ingest.IngestionResult;
import com.shaahil.resumeapi.ai.ingest.IngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(AiTestEmbeddingConfiguration.class)
class IngestionServiceIT extends PostgresTestContainerConfig {

    private static final List<String> FIXTURE_NAMES = List.of(
            "education-sample.md",
            "experience-acme-engineer.md",
            "experience-acme-lead.md",
            "project-acme-search.md",
            "skills-sample.md"
    );

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DeterministicEmbeddingModel embeddingModel;

    private Path fixtureDirectory;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.update("DELETE FROM vector_store");
        fixtureDirectory = Files.createTempDirectory("knowledge-fixtures-");
        for (String fixtureName : FIXTURE_NAMES) {
            ClassPathResource resource = new ClassPathResource("knowledge-fixtures/" + fixtureName);
            Files.copy(resource.getInputStream(), fixtureDirectory.resolve(fixtureName));
        }
        embeddingModel.reset();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fixtureDirectory != null) {
            try (var paths = Files.walk(fixtureDirectory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException exception) {
                        throw new RuntimeException(exception);
                    }
                });
            }
        }
    }

    @Test
    void ingestsFixtureChunks() {
        IngestionResult result = ingestionService.ingest(pattern());

        assertThat(result).isEqualTo(new IngestionResult(5, 0, 0, 0));
        assertThat(embeddingModel.embeddedTexts()).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(5);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT metadata->>'key' AS chunk_key, metadata->>'contentHash' AS content_hash, "
                        + "metadata->>'embeddingModel' AS embedding_model, metadata->>'embeddingDims' AS embedding_dims "
                        + "FROM vector_store");
        assertThat(rows).hasSize(5).allSatisfy(row -> {
            assertThat(row.get("chunk_key")).isNotNull();
            assertThat(row.get("content_hash")).isNotNull();
            assertThat(row.get("embedding_model")).isEqualTo("test-embedding-model");
            assertThat(row.get("embedding_dims")).isEqualTo("1536");
        });
    }

    @Test
    void secondRunChangesNothing() {
        IngestionResult firstRun = ingestionService.ingest(pattern());
        int embeddedBefore = embeddingModel.embeddedTexts();
        assertThat(firstRun.added()).isEqualTo(5);
        assertThat(embeddedBefore).isEqualTo(5);

        IngestionResult result = ingestionService.ingest(pattern());

        assertThat(result).isEqualTo(new IngestionResult(0, 0, 5, 0));
        assertThat(embeddingModel.embeddedTexts() - embeddedBefore).isEqualTo(0);
    }

    @Test
    void changedContentIsUpserted() throws IOException {
        ingestionService.ingest(pattern());
        Map<String, String> before = hashesByKey();
        int embeddedBefore = embeddingModel.embeddedTexts();
        Path changedFile = fixtureDirectory.resolve("experience-acme-engineer.md");
        Files.writeString(changedFile, Files.readString(changedFile) + "\nChanged content.", StandardCharsets.UTF_8);

        IngestionResult result = ingestionService.ingest(pattern());
        Map<String, String> after = hashesByKey();

        assertThat(result).isEqualTo(new IngestionResult(0, 1, 4, 0));
        assertThat(embeddingModel.embeddedTexts() - embeddedBefore).isEqualTo(1);
        assertThat(after).hasSize(5);
        assertThat(after.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(before.get(entry.getKey())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()))
                .containsExactly("experience:acme-corp:example-engineer:0");
    }

    @Test
    void changedDocVersionAloneChangesNothing() throws IOException {
        ingestionService.ingest(pattern());
        int embeddedBefore = embeddingModel.embeddedTexts();
        Path changedFile = fixtureDirectory.resolve("experience-acme-engineer.md");
        Files.writeString(changedFile, Files.readString(changedFile).replace("docVersion: 1", "docVersion: 2"),
                StandardCharsets.UTF_8);

        IngestionResult result = ingestionService.ingest(pattern());

        assertThat(result).isEqualTo(new IngestionResult(0, 0, 5, 0));
        assertThat(embeddingModel.embeddedTexts() - embeddedBefore).isEqualTo(0);
    }

    @Test
    void removedFileDeletesOrphan() throws IOException {
        ingestionService.ingest(pattern());
        int embeddedBefore = embeddingModel.embeddedTexts();
        Files.delete(fixtureDirectory.resolve("project-acme-search.md"));

        IngestionResult result = ingestionService.ingest(pattern());

        assertThat(result).isEqualTo(new IngestionResult(0, 0, 4, 1));
        assertThat(embeddingModel.embeddedTexts() - embeddedBefore).isEqualTo(0);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class)).isEqualTo(4);
        assertThat(hashesByKey()).doesNotContainKey("project:acme-corp:example-project-builder:0");
    }

    @Test
    void similaritySearchReturnsScores() {
        ingestionService.ingest(pattern());

        String storedText = jdbcTemplate.queryForObject(
                "SELECT content FROM vector_store ORDER BY id LIMIT 1", String.class);
        SearchRequest request = SearchRequest.builder().query(storedText).topK(5).build();
        List<Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            System.out.println("Search diagnostics: threshold=" + request.getSimilarityThreshold());
            System.out.println("Search diagnostics: rowCount="
                    + jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class));
            System.out.println("Search diagnostics: rawDistances=" + jdbcTemplate.queryForList(
                    "SELECT id, embedding <=> (SELECT embedding FROM vector_store WHERE content = ?) "
                            + "AS raw_distance FROM vector_store ORDER BY raw_distance", storedText));
        }

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().getText()).isEqualTo(storedText);
        assertThat(results).allSatisfy(result -> {
            System.out.println("Similarity result score=" + result.getScore());
            assertThat(result.getScore()).isBetween(0.0, 1.0);
        });
    }

    private String pattern() {
        return fixtureDirectory.toUri() + "*.md";
    }

    private Map<String, String> hashesByKey() {
        return jdbcTemplate.query("SELECT metadata->>'key' AS chunk_key, metadata->>'contentHash' AS content_hash "
                        + "FROM vector_store", (resultSet, rowNumber) -> Map.entry(
                        resultSet.getString("chunk_key"), resultSet.getString("content_hash")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
