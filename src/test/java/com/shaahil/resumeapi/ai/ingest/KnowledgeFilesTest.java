package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeFilesTest {

    private static final Pattern WHOLE_WORD_FORBIDDEN =
            Pattern.compile("\\b(shipped|live|deployed|production)\\b", Pattern.CASE_INSENSITIVE);

    @Test
    void deepDiveKnowledgeFilesAreCompleteAndSafe() throws IOException {
        KnowledgeFileParser parser = new KnowledgeFileParser();
        List<KnowledgeDocument> documents = parser.loadAll(IngestionService.DEFAULT_LOCATION);
        Set<String> keys = new HashSet<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Set<String> alightSections = documents.stream()
                .filter(document -> "Alight Solutions".equals(document.company()))
                .map(KnowledgeDocument::section)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(documents).hasSize(17);
        assertThat(documents.stream()
                .filter(document -> "Alight Solutions".equals(document.company())
                        || ("architecture".equals(document.section())
                        && (document.company() == null || document.company().isBlank()))))
                .hasSize(8);
        assertThat(documents.stream().filter(document -> "Wipro".equals(document.company()))).hasSize(3);
        assertThat(documents.stream().filter(document -> "Tata Consultancy Services".equals(document.company()))).hasSize(4);
        assertThat(documents.stream().filter(document -> document.company() == null
                && ("practices".equals(document.section()) || "skills".equals(document.section())))).hasSize(2);
        assertThat(alightSections).containsExactlyInAnyOrder(
                "experience", "ai-work", "performance", "decisions", "delivery", "lessons", "production-support");
        assertThat(documents.stream()
                .filter(document -> "architecture".equals(document.section())
                        && (document.company() == null || document.company().isBlank()))).hasSize(1);

        for (KnowledgeDocument document : documents) {
            assertThat(keys.add(ChunkFactory.key(document, 0))).isTrue();
            assertThat(document.body()).isNotBlank();
            assertThat(document.body().trim().split("\\s+")).hasSizeLessThan(350);
            assertThat(document.skills()).isNotNull().isInstanceOf(List.class);
        }

        for (Resource resource : resolver.getResources(IngestionService.DEFAULT_LOCATION)) {
            String content = resource.getContentAsString(StandardCharsets.UTF_8);
            String lowerCaseContent = content.toLowerCase(Locale.ROOT);
            KnowledgeDocument document = parser.parse(content);
            String filename = resource.getFilename();

            assertThat(lowerCaseContent)
                    .doesNotContain("[placeholder]", "salary", "licence", "police", "clearance",
                            "notice period", "deutsche", "@", "http");
            assertThat(document.skills()).isNotNull().isInstanceOf(List.class);

            if ("ai-work".equals(document.section())) {
                assertThat(document.body().toLowerCase(Locale.ROOT)).contains("proof of concept");
                assertThat(WHOLE_WORD_FORBIDDEN.matcher(document.body()).find()).isFalse();
            }

            if ("Alight Solutions".equals(document.company())
                    || ("architecture".equals(document.section())
                    && (document.company() == null || document.company().isBlank()))) {
                assertThat(Pattern.compile("\\bcluster\\b", Pattern.CASE_INSENSITIVE)
                        .matcher(content).find()).isFalse();
            }

            if (Pattern.compile("\\bintroduced\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find()) {
                assertThat(document.company()).isEqualTo("Wipro");
            }
            if (Pattern.compile("\\bKubernetes\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find()) {
                assertThat(filename).isIn("architecture-cba.md", "skills-evidence.md");
            }
            if (Pattern.compile("\\bqdrant\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find()) {
                assertThat(filename).isEqualTo("skills-evidence.md");
            }
            if (Pattern.compile("\\bOracle\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find()) {
                assertThat("Tata Consultancy Services".equals(document.company())
                        || "skills-evidence.md".equals(filename)).isTrue();
            }
            if (Pattern.compile("\\bRabbitMQ\\b", Pattern.CASE_INSENSITIVE)
                    .matcher(content).find()) {
                assertThat(document.company()).isEqualTo("Tata Consultancy Services");
            }
        }
    }
}
