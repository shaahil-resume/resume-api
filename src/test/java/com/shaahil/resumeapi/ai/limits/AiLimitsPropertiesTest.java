package com.shaahil.resumeapi.ai.limits;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AiLimitsPropertiesTest {

    @Test
    void defaultsApplyWhenNothingIsSet() {
        assertThat(new AiLimitsProperties(null, null, null))
                .isEqualTo(new AiLimitsProperties(10, 60, 500));
    }

    @Test
    void perMinuteMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiLimitsProperties(0, 60, 500))
                .withMessageContaining("ai.limits.perMinute");
    }

    @Test
    void perHourMustNotBeLowerThanPerMinute() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiLimitsProperties(10, 5, 500))
                .withMessageContaining("ai.limits.perHour");
    }

    @Test
    void dailyModelCallsMustBePositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiLimitsProperties(10, 60, 0))
                .withMessageContaining("ai.limits.dailyModelCalls");
    }
}
