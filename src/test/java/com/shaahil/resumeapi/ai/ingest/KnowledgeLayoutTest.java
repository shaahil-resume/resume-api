package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLayoutTest {

    @Test
    void defaultLocationAndRootKnowledgeLayoutAreCorrect() throws IOException {
        assertThat(IngestionService.DEFAULT_LOCATION)
                .isEqualTo("classpath*:knowledge/deep-dive/*.md");

        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:knowledge/*.md");
        Set<String> filenames = Arrays.stream(resources)
                .map(Resource::getFilename)
                .collect(Collectors.toSet());

        assertThat(filenames).containsExactlyInAnyOrder("quick-facts.md", "README.md");
    }
}
