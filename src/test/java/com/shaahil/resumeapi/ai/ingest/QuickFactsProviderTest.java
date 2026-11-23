package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class QuickFactsProviderTest {

    @Test
    void stripsFrontMatter() {
        QuickFactsProvider provider = new QuickFactsProvider(new ByteArrayResource(
                "---\nsection: quick-facts\n---\nCandidate builds reliable services.".getBytes()));

        assertThat(provider.text()).isEqualTo("Candidate builds reliable services.");
    }

    @Test
    void readsPlainTextWithoutFrontMatter() {
        QuickFactsProvider provider = new QuickFactsProvider(new ByteArrayResource(
                "  Candidate enjoys backend engineering.  ".getBytes()));

        assertThat(provider.text()).isEqualTo("Candidate enjoys backend engineering.");
    }

    @Test
    void realQuickFactsContainTechnologyTimeline() {
        QuickFactsProvider provider = new QuickFactsProvider(new ClassPathResource("knowledge/quick-facts.md"));

        assertThat(provider.text())
                .contains("Technology timeline")
                .contains("MongoDB")
                .contains("Kubernetes")
                .contains("JUnit and Mockito");
    }

    @Test
    void missingFileFailsClearly() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new QuickFactsProvider(
                        new ClassPathResource("knowledge-fixtures/missing-quick-facts.md")))
                .withMessageContaining("quick-facts");
    }
}
