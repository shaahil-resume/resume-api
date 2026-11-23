package com.shaahil.resumeapi.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Model prices in US dollars per one million tokens, used to estimate what a chat request cost.
 * The defaults are gpt-4o-mini list prices; update them when the chat model changes.
 */
@ConfigurationProperties(prefix = "analytics.pricing")
public record AnalyticsPricingProperties(BigDecimal inputPerMillionTokens, BigDecimal outputPerMillionTokens) {

    public AnalyticsPricingProperties {
        inputPerMillionTokens = inputPerMillionTokens == null ? new BigDecimal("0.15") : inputPerMillionTokens;
        outputPerMillionTokens = outputPerMillionTokens == null ? new BigDecimal("0.60") : outputPerMillionTokens;

        if (inputPerMillionTokens.signum() < 0) {
            throw new IllegalArgumentException(
                    "analytics.pricing.inputPerMillionTokens must not be negative: " + inputPerMillionTokens);
        }
        if (outputPerMillionTokens.signum() < 0) {
            throw new IllegalArgumentException(
                    "analytics.pricing.outputPerMillionTokens must not be negative: " + outputPerMillionTokens);
        }
    }
}
