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
                .contains("What has he built with Spring Boot?")
                .contains("Where is he based?")
                .contains("What are his main skills?")
                .doesNotContainIgnoringCase("lack")
                .doesNotContainIgnoringCase("does not have")
                .doesNotContainIgnoringCase("doesn't have");
    }
}
