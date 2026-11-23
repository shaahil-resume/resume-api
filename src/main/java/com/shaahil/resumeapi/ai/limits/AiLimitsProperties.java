package com.shaahil.resumeapi.ai.limits;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cost and abuse guards for the chat endpoint. Defaults suit a single instance.
 *
 * @param perMinute      questions one visitor may ask in any rolling minute
 * @param perHour        questions one visitor may ask in any rolling hour
 * @param dailyModelCalls model calls allowed per UTC day across all visitors
 */
@ConfigurationProperties(prefix = "ai.limits")
public record AiLimitsProperties(Integer perMinute, Integer perHour, Integer dailyModelCalls) {

    public AiLimitsProperties {
        perMinute = perMinute == null ? 10 : perMinute;
        perHour = perHour == null ? 60 : perHour;
        dailyModelCalls = dailyModelCalls == null ? 500 : dailyModelCalls;

        if (perMinute < 1) {
            throw new IllegalArgumentException("ai.limits.perMinute must be at least 1: " + perMinute);
        }
        if (perHour < perMinute) {
            throw new IllegalArgumentException(
                    "ai.limits.perHour must not be lower than ai.limits.perMinute: " + perHour + " < " + perMinute);
        }
        if (dailyModelCalls < 1) {
            throw new IllegalArgumentException("ai.limits.dailyModelCalls must be at least 1: " + dailyModelCalls);
        }
    }
}
