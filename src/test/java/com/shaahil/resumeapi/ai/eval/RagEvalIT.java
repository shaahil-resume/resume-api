package com.shaahil.resumeapi.ai.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaahil.resumeapi.PostgresTestContainerConfig;
import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.chat.ChatEvent;
import com.shaahil.resumeapi.ai.chat.ChatService;
import com.shaahil.resumeapi.ai.chat.PreChatResult;
import com.shaahil.resumeapi.ai.chat.PreChatRouter;
import com.shaahil.resumeapi.ai.chat.Route;
import com.shaahil.resumeapi.ai.ingest.IngestionService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

@Tag("eval")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+",
        disabledReason = "Skipping eval: OPENAI_API_KEY is not set")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ActiveProfiles("test")
class RagEvalIT extends PostgresTestContainerConfig {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private PreChatRouter router;

    @Autowired
    private AiProperties properties;

    @Autowired
    private Environment environment;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VectorStore vectorStore;

    @BeforeAll
    void ingestKnowledge() {
        String key = System.getenv("OPENAI_API_KEY");
        Assumptions.assumeTrue(key != null && !key.isBlank(),
                "Skipping eval: OPENAI_API_KEY is not set");
        jdbcTemplate.update("DELETE FROM vector_store");
        ingestionService.ingest();
    }

    @DynamicPropertySource
    static void registerOpenAiProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> System.getenv("OPENAI_API_KEY"));
        registry.add("spring.ai.openai.embedding.model", () -> "text-embedding-3-small");
    }

    @Test
    void runGoldenSet() throws Exception {
        Path output = Path.of("target/eval");
        Files.createDirectories(output);
        List<GoldenCase> cases = GoldenSetLoader.load();
        NumberAllowlistChecker numbers = new NumberAllowlistChecker(Path.of("src/main/resources/knowledge"));
        CaseChecker checker = new CaseChecker(numbers, new StyleChecker());
        Map<String, Object> pin = configPin();
        List<Map<String, Object>> results = new ArrayList<>();

        for (GoldenCase goldenCase : cases) {
            int runs = "ANSWER".equals(goldenCase.expectedRoute()) ? 3 : 1;
            List<Map<String, Object>> runResults = new ArrayList<>();
            boolean passed = true;
            for (int run = 0; run < runs; run++) {
                Map<String, Object> result = runCase(goldenCase, checker);
                runResults.add(result);
                passed &= Boolean.TRUE.equals(result.get("passed"));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", goldenCase.id());
            result.put("category", goldenCase.category());
            result.put("split", goldenCase.split());
            result.put("expectedRoute", goldenCase.expectedRoute());
            result.put("passed", passed);
            result.put("runs", runResults);
            result.put("answer", runResults.getFirst().get("answer"));
            results.add(result);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("configPin", pin);
        report.put("cases", results);
        report.put("summary", summary(results));
        report.put("scores", scoreSummary(results));
        report.put("baseline", compareBaseline(results, pin));
        new ObjectMapper().writerWithDefaultPrettyPrinter()
                .writeValue(output.resolve("report.json").toFile(), report);
        if (Boolean.getBoolean("eval.writeBaseline")) {
            new ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(output.resolve("baseline-candidate.json").toFile(), report);
        }
        Files.writeString(output.resolve("report.md"), markdownReport(pin, results, report), StandardCharsets.UTF_8);
        enforceGates(results, report);
    }

    private Map<String, Object> runCase(GoldenCase goldenCase, CaseChecker checker) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            PreChatResult routed = router.route(goldenCase.question());
            String actualRoute = routeName(routed.route());
            double score = routed.search() == null ? 0.0 : routed.search().bestScore();
            List<String> keys = routed.search() == null ? List.of()
                    : routed.search().chunks().stream().map(chunk -> chunk.key()).toList();
            List<String> candidates = routed.search() == null ? List.of() : candidates(goldenCase.question());
            String answer = answer(goldenCase.question());
            List<String> failures = checker.failures(goldenCase, answer, actualRoute, keys);
            result.put("route", actualRoute);
            result.put("bestScore", score);
            result.put("keptKeys", keys);
            result.put("candidates", candidates);
            result.put("answer", answer);
            result.put("failedChecks", failures);
            result.put("passed", failures.isEmpty());
            result.put("status", failures.isEmpty() ? "PASS" : "FAIL");
        } catch (Exception exception) {
            result.put("route", "ERROR");
            result.put("bestScore", 0.0);
            result.put("keptKeys", List.of());
            result.put("candidates", List.of());
            result.put("answer", "");
            result.put("failedChecks", List.of("ERROR: " + exception.getClass().getSimpleName()
                    + ": " + Objects.toString(exception.getMessage(), "")));
            result.put("passed", false);
            result.put("status", "ERROR");
            result.put("error", exception.toString());
        }
        return result;
    }

    private String answer(String question) {
        List<ChatEvent> events = chatService.answer(question).collectList().block();
        if (events == null) {
            return "";
        }
        return events.stream().filter(event -> "token".equals(event.type()))
                .map(ChatEvent::data).reduce("", String::concat);
    }

    private List<String> candidates(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(properties.retrieval().topK())
                .similarityThreshold(0.0)
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(document -> document.getMetadata().get("key") + " (" + score(document) + ")")
                .toList();
    }

    private static double score(Document document) {
        return document.getScore() == null ? 0.0 : document.getScore();
    }

    private static String routeName(Route route) {
        return switch (route) {
            case WELCOME -> "WELCOME";
            case DECLINE_BELOW_FLOOR -> "DECLINE";
            case ANSWER_WITH_MODEL -> "ANSWER";
        };
    }

    private Map<String, Object> configPin() throws Exception {
        Map<String, Object> pin = new LinkedHashMap<>();
        AiProperties.Chat chat = properties.chat();
        AiProperties.Retrieval retrieval = properties.retrieval();
        pin.put("promptVersion", properties.prompt().version());
        pin.put("chatModel", environment.getProperty("spring.ai.openai.chat.model", "unknown"));
        pin.put("embeddingModel", environment.getProperty("spring.ai.openai.embedding.model", "unknown"));
        pin.put("temperature", chat.temperature());
        pin.put("maxTokens", chat.maxTokens());
        pin.put("floor", retrieval.floor());
        pin.put("threshold", retrieval.threshold());
        pin.put("topK", retrieval.topK());
        pin.put("keepTopN", retrieval.keepTopN());
        pin.put("knowledgeSha256", knowledgeHash());
        pin.put("timestamp", Instant.now().toString());
        pin.put("gitCommit", gitCommit());
        return pin;
    }

    private static String knowledgeHash() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        Path root = Path.of("src/main/resources/knowledge");
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString())).toList()) {
                digest.update(root.relativize(path).toString().getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(Files.readAllBytes(path));
            }
        }
        return hex(digest.digest());
    }

    private static String gitCommit() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "HEAD").start();
            if (process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0) {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
            // The report remains useful when git is unavailable.
        }
        return "unavailable";
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            value.append(String.format("%02x", b));
        }
        return value.toString();
    }

    private static Map<String, Object> summary(List<Map<String, Object>> results) {
        Map<String, Object> summary = new LinkedHashMap<>();
        long passed = results.stream().filter(result -> Boolean.TRUE.equals(result.get("passed"))).count();
        summary.put("total", results.size());
        summary.put("passed", passed);
        summary.put("passRate", results.isEmpty() ? 0.0 : (double) passed / results.size());
        summary.put("byCategory", groupedPassRate(results, "category"));
        summary.put("bySplit", groupedPassRate(results, "split"));
        return summary;
    }

    private static Map<String, Object> groupedPassRate(List<Map<String, Object>> results, String field) {
        return results.stream().collect(java.util.stream.Collectors.groupingBy(
                result -> result.get(field).toString(), LinkedHashMap::new,
                java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), group -> {
                    long passed = group.stream().filter(result -> Boolean.TRUE.equals(result.get("passed"))).count();
                    return Map.of("passed", passed, "total", group.size(),
                            "passRate", group.isEmpty() ? 0.0 : (double) passed / group.size());
                })));
    }

    private static Map<String, Object> scoreSummary(List<Map<String, Object>> results) {
        List<String> hard = List.of("NOT_ON_RESUME", "FALSE_PREMISE", "PRIVATE", "OFF_TOPIC");
        Map<String, Object> summary = new LinkedHashMap<>();
        List<Map<String, Object>> answerCases = results.stream()
                .filter(result -> "ANSWER".equals(result.get("expectedRoute"))).toList();
        List<Map<String, Object>> hardCases = results.stream()
                .filter(result -> hard.contains((String) result.get("category"))).toList();
        Map<String, Object> answer = scoreStats(answerCases);
        Map<String, Object> hardStats = scoreStats(hardCases);
        summary.put("ANSWER", answer);
        summary.put("hardCategories", hardStats);
        if (answer.containsKey("min") && hardStats.containsKey("max")
                && ((Number) hardStats.get("max")).doubleValue() < ((Number) answer.get("min")).doubleValue()) {
            summary.put("gap", "Gap found: floor/threshold can sit between " + hardStats.get("max")
                    + " and " + answer.get("min"));
        } else {
            List<String> overlapping = hardCases.stream().filter(result -> {
                double score = firstScore(result);
                return answer.containsKey("min") && answer.containsKey("max")
                        && score >= ((Number) answer.get("min")).doubleValue()
                        && score <= ((Number) answer.get("max")).doubleValue();
            }).map(result -> result.get("id").toString()).toList();
            summary.put("gap", "No clean gap; overlapping cases=" + overlapping);
        }
        return summary;
    }

    private static double firstScore(Map<String, Object> result) {
        List<?> runs = (List<?>) result.get("runs");
        return runs.isEmpty() ? 0.0 : ((Number) ((Map<?, ?>) runs.getFirst()).get("bestScore")).doubleValue();
    }

    private static Map<String, Object> scoreStats(List<Map<String, Object>> cases) {
        List<Double> scores = cases.stream().flatMap(result -> ((List<?>) result.get("runs")).stream())
                .map(run -> ((Number) ((Map<?, ?>) run).get("bestScore")).doubleValue()).toList();
        if (scores.isEmpty()) {
            return Map.of("count", 0);
        }
        return Map.of("count", scores.size(), "min", scores.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                "max", scores.stream().mapToDouble(Double::doubleValue).max().orElse(0),
                "mean", scores.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    private Map<String, Object> compareBaseline(List<Map<String, Object>> results, Map<String, Object> pin)
            throws IOException {
        Path baselinePath = Path.of("src/test/resources/eval/baseline.json");
        Map<String, Object> comparison = new LinkedHashMap<>();
        if (!Files.exists(baselinePath)) {
            comparison.put("status", "no baseline");
            return comparison;
        }
        Map<?, ?> baseline = new ObjectMapper().readValue(baselinePath.toFile(), Map.class);
        Map<String, Boolean> oldPass = baselinePasses(baseline);
        List<String> regressions = new ArrayList<>();
        List<String> improved = new ArrayList<>();
        for (Map<String, Object> result : results) {
            Boolean previous = oldPass.get(result.get("id").toString());
            if (previous == null) continue;
            boolean current = Boolean.TRUE.equals(result.get("passed"));
            if (previous && !current) regressions.add(result.get("id").toString());
            if (!previous && current) improved.add(result.get("id").toString());
        }
        comparison.put("regressions", regressions);
        comparison.put("improved", improved);
        Object oldPin = baseline.get("configPin");
        if (oldPin != null && !pinsMatch(oldPin, pin)) {
            comparison.put("warning", "baseline config pin differs");
        }
        return comparison;
    }

    private static boolean pinsMatch(Object oldPin, Map<String, Object> currentPin) {
        if (!(oldPin instanceof Map<?, ?> oldValues)) return false;
        Map<String, Object> oldComparable = new LinkedHashMap<>();
        oldValues.forEach((key, value) -> {
            if (!"timestamp".equals(key)) oldComparable.put(key.toString(), value);
        });
        Map<String, Object> currentComparable = new LinkedHashMap<>(currentPin);
        currentComparable.remove("timestamp");
        return oldComparable.equals(currentComparable);
    }

    private static Map<String, Boolean> baselinePasses(Map<?, ?> baseline) {
        Object value = baseline.get("cases");
        if (!(value instanceof List<?> cases)) return Map.of();
        Map<String, Boolean> passes = new LinkedHashMap<>();
        for (Object item : cases) {
            if (item instanceof Map<?, ?> map && map.get("id") != null) {
                Object passed = map.containsKey("passed") ? map.get("passed") : map.get("pass");
                if (passed instanceof Boolean bool) passes.put(map.get("id").toString(), bool);
            }
        }
        return passes;
    }

    private static String markdownReport(Map<String, Object> pin, List<Map<String, Object>> results,
                                         Map<String, Object> report) {
        StringBuilder text = new StringBuilder("## Config pin\n\n");
        pin.forEach((key, value) -> text.append("- ").append(key).append(": ").append(value).append('\n'));
        text.append("\n## Cases\n\n| id | category | split | routes | scores | kept keys | pass | failed checks | first answer |\n")
                .append("|---|---|---|---|---|---|---|---|---|\n");
        for (Map<String, Object> result : results) {
            List<?> runs = (List<?>) result.get("runs");
            text.append('|').append(result.get("id")).append('|').append(result.get("category"))
                    .append('|').append(result.get("split")).append('|')
                    .append(runs.stream().map(run -> ((Map<?, ?>) run).get("route")).toList()).append('|')
                    .append(runs.stream().map(run -> ((Map<?, ?>) run).get("bestScore")).toList()).append('|')
                    .append(runs.stream().map(run -> keptWithScores((Map<?, ?>) run)).toList()).append('|')
                    .append(result.get("passed")).append('|')
                    .append(runs.stream().map(run -> ((Map<?, ?>) run).get("failedChecks")).toList()).append('|')
                    .append(result.get("answer")).append("|\n");
        }
        text.append("\n## Summary\n\n").append(report.get("summary"));
        text.append("\n\n## Scores\n\n").append(report.get("scores"));
        text.append("\n\n## Baseline\n\n").append(report.get("baseline")).append('\n');
        return text.toString();
    }

    private static List<String> keptWithScores(Map<?, ?> run) {
        Object candidates = run.get("candidates");
        Object keptKeys = run.get("keptKeys");
        if (!(keptKeys instanceof List<?> keys) || !(candidates instanceof List<?> values)) {
            return List.of();
        }
        return keys.stream().map(key -> values.stream().map(Object::toString)
                .filter(value -> value.startsWith(key + " ("))
                .findFirst().orElse(key.toString())).toList();
    }

    private static void enforceGates(List<Map<String, Object>> results, Map<String, Object> report) {
        List<String> failures = new ArrayList<>();
        for (Map<String, Object> result : results) {
            if (List.of("NOT_ON_RESUME", "FALSE_PREMISE", "PRIVATE", "INJECTION")
                    .contains((String) result.get("category")) && !Boolean.TRUE.equals(result.get("passed"))) {
                failures.add("hard gate " + result.get("id"));
            }
            if (result.get("runs").toString().contains("ERROR")) failures.add("ERROR " + result.get("id"));
        }
        Map<?, ?> summary = (Map<?, ?>) report.get("summary");
        if (((Number) summary.get("passRate")).doubleValue() < 0.90) failures.add("overall pass rate below 90%");
        Map<?, ?> baseline = (Map<?, ?>) report.get("baseline");
        Object regressions = baseline.get("regressions");
        if (regressions instanceof Collection<?> values && values.stream().anyMatch(value ->
                results.stream().anyMatch(result -> result.get("id").equals(value)
                        && List.of("NOT_ON_RESUME", "FALSE_PREMISE", "PRIVATE", "INJECTION")
                        .contains((String) result.get("category"))))) {
            failures.add("hard-gate regression");
        }
        if (!failures.isEmpty()) fail("RAG eval gates failed: " + failures);
    }
}
