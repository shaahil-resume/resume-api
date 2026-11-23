package com.shaahil.resumeapi.ai.limits;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-24T00:00:00Z"));

    private VisitorRateLimiter limiter(int perMinute, int perHour) {
        return new VisitorRateLimiter(new AiLimitsProperties(perMinute, perHour, 500), clock);
    }

    @Test
    void allowsUpToThePerMinuteLimitThenRefusesWithRetryAfter() {
        VisitorRateLimiter limiter = limiter(3, 60);

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.check("1.1.1.1").allowed()).isTrue();
            clock.advance(Duration.ofSeconds(1));
        }

        VisitorRateLimiter.Decision refused = limiter.check("1.1.1.1");
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isEqualTo(57);
    }

    @Test
    void allowsAgainOnceTheMinuteHasPassed() {
        VisitorRateLimiter limiter = limiter(2, 60);
        limiter.check("1.1.1.1");
        limiter.check("1.1.1.1");
        assertThat(limiter.check("1.1.1.1").allowed()).isFalse();

        clock.advance(Duration.ofSeconds(61));

        assertThat(limiter.check("1.1.1.1").allowed()).isTrue();
    }

    @Test
    void refusedQuestionsAreNotCounted() {
        VisitorRateLimiter limiter = limiter(1, 60);
        limiter.check("1.1.1.1");
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.check("1.1.1.1").allowed()).isFalse();
        }

        clock.advance(Duration.ofSeconds(60));

        assertThat(limiter.check("1.1.1.1").allowed()).isTrue();
    }

    @Test
    void visitorsAreLimitedSeparately() {
        VisitorRateLimiter limiter = limiter(1, 60);
        assertThat(limiter.check("1.1.1.1").allowed()).isTrue();
        assertThat(limiter.check("1.1.1.1").allowed()).isFalse();

        assertThat(limiter.check("2.2.2.2").allowed()).isTrue();
    }

    @Test
    void perHourLimitAppliesEvenWhenEachMinuteIsFine() {
        VisitorRateLimiter limiter = limiter(10, 12);
        for (int i = 0; i < 12; i++) {
            assertThat(limiter.check("1.1.1.1").allowed()).isTrue();
            clock.advance(Duration.ofMinutes(2));
        }

        VisitorRateLimiter.Decision refused = limiter.check("1.1.1.1");
        assertThat(refused.allowed()).isFalse();
        // The oldest question was 24 minutes ago, so it leaves the hour window in 36 minutes.
        assertThat(refused.retryAfterSeconds()).isEqualTo(36 * 60);
    }
}
