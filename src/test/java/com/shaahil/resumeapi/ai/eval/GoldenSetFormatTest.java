package com.shaahil.resumeapi.ai.eval;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class GoldenSetFormatTest {

    private static final Set<String> CATEGORIES = Set.of(
            "ANSWERABLE", "YEARS", "NOT_ON_RESUME", "FALSE_PREMISE", "UNDERSPECIFIED",
            "NONSENSICAL", "PRIVATE", "MODALITY", "INJECTION", "OFF_TOPIC", "GREETING", "VAGUE");

    @Test
    void goldenSetHasValidOrderedCases() {
        var cases = GoldenSetLoader.load();

        assertThat(cases).hasSizeGreaterThanOrEqualTo(35);
        assertThat(cases.stream().map(GoldenCase::id)).doesNotHaveDuplicates();
        assertThat(cases).allSatisfy(goldenCase -> {
            int index = cases.indexOf(goldenCase) + 1;
            assertThat(goldenCase.id()).isEqualTo("g%02d".formatted(index));
            assertThat(goldenCase.category()).isIn(CATEGORIES);
            assertThat(goldenCase.split()).isIn("tune", "holdout");
            assertThat(goldenCase.expectedRoute()).isIn("ANSWER", "DECLINE", "WELCOME");
            goldenCase.mustMatch().forEach(regex -> Pattern.compile(regex));
            goldenCase.mustNotMatch().forEach(regex -> Pattern.compile(regex));
            if (goldenCase.category().equals("ANSWERABLE")) {
                assertThat(goldenCase.expectedKeyContains().isEmpty()
                        && goldenCase.mustMatch().isEmpty()).isFalse();
            }
        });

        assertThat(cases.stream().filter(goldenCase -> goldenCase.split().equals("holdout")).count())
                .isGreaterThanOrEqualTo((long) Math.ceil(cases.size() * 0.20));
        assertThat(cases.stream().map(GoldenCase::category).distinct())
                .containsExactlyInAnyOrderElementsOf(CATEGORIES);
    }
}
