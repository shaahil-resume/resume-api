package com.shaahil.resumeapi.ai.limits;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DailyModelCallCapTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-24T10:00:00Z"));

    @Test
    void refusesOnceTheDailyAllowanceIsUsed() {
        DailyModelCallCap cap = new DailyModelCallCap(new AiLimitsProperties(10, 60, 2), clock);

        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();
    }

    @Test
    void allowanceResetsAtTheNextUtcDay() {
        DailyModelCallCap cap = new DailyModelCallCap(new AiLimitsProperties(10, 60, 1), clock);
        assertThat(cap.tryAcquire()).isTrue();
        assertThat(cap.tryAcquire()).isFalse();

        clock.advance(Duration.ofHours(14).plusMinutes(1));

        assertThat(cap.tryAcquire()).isTrue();
    }
}
