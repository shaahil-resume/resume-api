package com.shaahil.resumeapi.ai.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyTemplatesTest {

    @Test
    void welcomeContainsExamplesWithoutNegativeSkillClaims() {
        assertTemplate(ReplyTemplates.welcome());
    }

    @Test
    void declineContainsExamplesWithoutNegativeSkillClaims() {
        assertTemplate(ReplyTemplates.declineBelowFloor());
    }

    private static void assertTemplate(String template) {
        assertThat(template).isNotBlank()
                .contains("What have I built with Spring Boot?")
                .contains("Where am I based?")
                .contains("What are my main skills?")
                .doesNotContainIgnoringCase("lack")
                .doesNotContainIgnoringCase("does not have")
                .doesNotContainIgnoringCase("doesn't have");
    }

    @Test
    void fallbackIsFirstPersonAndPointsToTheContactForm() {
        assertThat(ReplyTemplates.fallback()).isNotBlank()
                .contains("try again")
                .contains("contact form")
                .doesNotContainIgnoringCase("exception")
                .doesNotContainIgnoringCase("error");
    }
}
