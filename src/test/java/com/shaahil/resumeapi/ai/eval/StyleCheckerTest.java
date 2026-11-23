package com.shaahil.resumeapi.ai.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StyleCheckerTest {

    private final StyleChecker checker = new StyleChecker();

    @Test
    void acceptsFirstPersonAnswerWithoutSourceLanguage() {
        assertThat(checker.passes("I used Java and Spring Boot at Wipro.")).isTrue();
    }

    @Test
    void rejectsCitationAndThirdPersonStyle() {
        assertThat(checker.passes("[quick_facts] He worked at Wipro. The material says so."))
                .isFalse();
    }
}
