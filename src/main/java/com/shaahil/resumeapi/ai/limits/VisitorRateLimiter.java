package com.shaahil.resumeapi.ai.limits;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding-window limiter per visitor, held in memory. That is correct for a single instance;
 * with several instances the counts would need to live in Redis or at a gateway.
 */
@Component
public class VisitorRateLimiter {

    private static final Duration MINUTE = Duration.ofMinutes(1);
    private static final Duration HOUR = Duration.ofHours(1);
    private static final long EVICT_EVERY_CALLS = 1_000;

    private final AiLimitsProperties limits;
    private final Clock clock;
    private final Map<String, ArrayDeque<Long>> calls = new ConcurrentHashMap<>();
    private final AtomicLong checks = new AtomicLong();

    public VisitorRateLimiter(AiLimitsProperties limits, Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    /** Records the question when allowed. A refused question is not recorded. */
    public Decision check(String visitor) {
        long now = clock.millis();
        if (checks.incrementAndGet() % EVICT_EVERY_CALLS == 0) {
            evictIdle(now);
        }
        ArrayDeque<Long> window = calls.computeIfAbsent(visitor, key -> new ArrayDeque<>());
        synchronized (window) {
            Long oldest = window.peekFirst();
            while (oldest != null && now - oldest >= HOUR.toMillis()) {
                window.pollFirst();
                oldest = window.peekFirst();
            }
            long retryAfterMillis = 0;
            if (oldest != null && window.size() >= limits.perHour()) {
                retryAfterMillis = oldest + HOUR.toMillis() - now;
            }
            long inLastMinute = window.stream().filter(time -> now - time < MINUTE.toMillis()).count();
            if (inLastMinute >= limits.perMinute()) {
                long oldestInMinute = window.stream().filter(time -> now - time < MINUTE.toMillis())
                        .findFirst().orElse(now);
                retryAfterMillis = Math.max(retryAfterMillis, oldestInMinute + MINUTE.toMillis() - now);
            }
            if (retryAfterMillis > 0) {
                return Decision.refused(Math.max(1, (retryAfterMillis + 999) / 1000));
            }
            window.addLast(now);
            return Decision.permitted();
        }
    }

    private void evictIdle(long now) {
        calls.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                Long newest = entry.getValue().peekLast();
                return newest == null || now - newest >= HOUR.toMillis();
            }
        });
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {

        static Decision permitted() {
            return new Decision(true, 0);
        }

        static Decision refused(long retryAfterSeconds) {
            return new Decision(false, retryAfterSeconds);
        }
    }
}
