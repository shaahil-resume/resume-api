package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class QuickFactsFileTest {

    @Test
    void quickFactsFileStaysShortAndSafe() throws IOException {
        String content = Files.readString(Path.of("src/main/resources/knowledge/quick-facts.md"));
        String lowerCaseContent = content.toLowerCase();
        long wordCount = content.trim().split("\\s+").length;

        assertThat(content).isNotBlank()
                .contains("Core areas:")
                .contains("proof of concept");
        assertThat(wordCount).isLessThan(650);
        assertThat(lowerCaseContent)
                .doesNotContain("salary")
                .doesNotContain("licence")
                .doesNotContain("police")
                .doesNotContain("clearance")
                .doesNotContain("notice period")
                .doesNotContain("available immediately")
                .doesNotContain("@")
                .doesNotContain("http");
    }
}
