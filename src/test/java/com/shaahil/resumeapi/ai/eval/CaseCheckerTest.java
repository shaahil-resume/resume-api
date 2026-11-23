package com.shaahil.resumeapi.ai.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseCheckerTest {

    private final CaseChecker checker = new CaseChecker(new NumberAllowlistChecker(), new StyleChecker());

    @Test
    void acceptsRouteKeyRegexAndCheckerRules() {
        GoldenCase goldenCase = new GoldenCase(
                "g01", "ANSWERABLE", "tune", "How did he speed up deployments?", "ANSWER",
                List.of("deployment:wipro"), List.of("90%"), List.of(), null);

        assertThat(checker.passes(goldenCase, "I used GitHub Actions and cut deployment time by 90%.",
                "ANSWER", List.of("deployment:wipro:senior-software-engineer:0"))).isTrue();
    }

    @Test
    void acceptsDeclineForNotOnResume() {
        GoldenCase goldenCase = new GoldenCase(
                "g16", "NOT_ON_RESUME", "tune", "What is his salary expectation?", "ANSWER",
                List.of(), List.of("(?i)(not on my resume|don't have)"), List.of(), null);

        assertThat(checker.failures(goldenCase, "I don't have that on my resume.",
                "DECLINE", List.of())).isEmpty();
    }

    @Test
    void rejectsWrongKeyInventedNumberAndCitation() {
        GoldenCase goldenCase = new GoldenCase(
                "g01", "ANSWERABLE", "tune", "How did he speed up deployments?", "ANSWER",
                List.of("deployment:wipro"), List.of("70%"), List.of(), null);

        assertThat(checker.passes(goldenCase, "[quick_facts] He improved performance by 70%.",
                "ANSWER", List.of("experience:wipro:senior-software-engineer:0"))).isFalse();
    }
}
