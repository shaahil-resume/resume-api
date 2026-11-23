package com.shaahil.resumeapi.ai.limits;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Caps model calls per UTC day across all visitors, the in-app backstop behind the
 * OpenAI project budget limit. The count lives in memory and resets on restart.
 */
@Component
public class DailyModelCallCap {

    private final AiLimitsProperties limits;
    private final Clock clock;
    private LocalDate day;
    private int used;

    public DailyModelCallCap(AiLimitsProperties limits, Clock clock) {
        this.limits = limits;
        this.clock = clock;
    }

    /** Takes one call from today's allowance; false when it is used up. */
    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= limits.dailyModelCalls()) {
            return false;
        }
        used++;
        return true;
    }
}
