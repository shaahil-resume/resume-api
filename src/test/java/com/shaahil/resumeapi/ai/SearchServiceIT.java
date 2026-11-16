package com.shaahil.resumeapi.ai;

import com.shaahil.resumeapi.PostgresTestContainerConfig;
import com.shaahil.resumeapi.ai.ingest.IngestionService;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import com.shaahil.resumeapi.ai.retrieval.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(AiTestEmbeddingConfiguration.class)
class SearchServiceIT extends PostgresTestContainerConfig {

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

    private Path fixtureDirectory;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.update("DELETE FROM vector_store");
        fixtureDirectory = Files.createTempDirectory("search-fixtures-");
        for (String fixtureName : FIXTURE_NAMES) {
            ClassPathResource resource = new ClassPathResource("knowledge-fixtures/" + fixtureName);
            Files.copy(resource.getInputStream(), fixtureDirectory.resolve(fixtureName));
        }
        ingestionService.ingest(pattern());
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
    void exactTextIsAboveFloorAndRanksItsChunkFirst() {
        Map<String, Object> stored = storedDocument();
        SearchOutcome outcome = searchService(0.1, 0.1, 10, 5).search((String) stored.get("content"));
        printScores("exact text", outcome);

        assertThat(outcome.aboveFloor()).isTrue();
        assertThat(outcome.bestScore()).isEqualTo(1.0);
        assertThat(outcome.chunks()).isNotEmpty();
        assertThat(outcome.chunks().getFirst().id()).isEqualTo(stored.get("id"));
        assertThat(outcome.chunks().getFirst().key()).isEqualTo(stored.get("chunk_key"));
    }

    @Test
    void highFloorDeclinesAnUnrelatedQuery() {
        SearchOutcome outcome = searchService(0.99, 0.99, 10, 5).search("zzzzzz qqqqqq");
        printScores("unrelated", outcome);

        assertThat(outcome.aboveFloor()).isFalse();
    }

    @Test
    void thresholdFiltersAndKeepTopNLimitsChunks() {
        SearchOutcome baseline = searchService(0.0, 0.0, 5, 5).search("Acme Corp");
        printScores("baseline", baseline);
        assertThat(baseline.chunks()).hasSizeGreaterThan(1);

        double threshold = Math.nextUp(baseline.chunks().getLast().score());
        SearchOutcome filtered = searchService(0.0, threshold, 5, 1).search("Acme Corp");
        printScores("threshold and keepTopN", filtered);

        assertThat(filtered.chunks()).hasSizeLessThan(baseline.chunks().size()).hasSizeLessThanOrEqualTo(1);
        assertThat(filtered.chunks()).allSatisfy(chunk -> assertThat(chunk.score()).isGreaterThanOrEqualTo(threshold));
    }

    @Test
    void topKIsRespected() {
        SearchOutcome outcome = searchService(0.0, 0.0, 3, 3).search("Acme Corp");
        printScores("topK", outcome);

        assertThat(outcome.chunks()).hasSizeLessThanOrEqualTo(3);
    }

    private SearchService searchService(double floor, double threshold, int topK, int keepTopN) {
        return new SearchService(vectorStore, properties(floor, threshold, topK, keepTopN));
    }

    private AiProperties properties(double floor, double threshold, int topK, int keepTopN) {
        return new AiProperties(new AiProperties.Retrieval(floor, threshold, topK, keepTopN), null, null);
    }

    private Map<String, Object> storedDocument() {
        return jdbcTemplate.queryForMap(
                "SELECT id::text AS id, content, metadata->>'key' AS chunk_key "
                        + "FROM vector_store ORDER BY id LIMIT 1");
    }

    private void printScores(String label, SearchOutcome outcome) {
        for (var chunk : outcome.chunks()) {
            System.out.println(label + " score=" + chunk.score() + " key=" + chunk.key());
        }
    }

    private String pattern() {
        return fixtureDirectory.toUri() + "*.md";
    }
}
