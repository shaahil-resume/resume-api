package com.shaahil.resumeapi.ai.eval;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumberAllowlistCheckerTest {

    private final NumberAllowlistChecker checker = new NumberAllowlistChecker();

    @Test
    void acceptsNumbersFromKnowledgeOrQuestion() {
        assertThat(checker.passes("Deployment time fell by 90%.", "How did deployments improve?"))
                .isTrue();
        assertThat(checker.passes("I have 15 years in that area.", "The question mentions 15 years."))
                .isTrue();
    }

    @Test
    void rejectsAnInventedNumber() {
        assertThat(checker.passes("Performance improved by 70%.", "How did performance improve?"))
                .isFalse();
    }
}
