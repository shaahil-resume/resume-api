package com.shaahil.resumeapi.analytics;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Turns token counts into an estimated cost in US dollars. */
@Component
public class CostEstimator {

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private final AnalyticsPricingProperties pricing;

    public CostEstimator(AnalyticsPricingProperties pricing) {
        this.pricing = pricing;
    }

    /** Null when the provider did not report token counts. */
    public BigDecimal estimate(Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null || completionTokens == null) {
            return null;
        }
        BigDecimal input = pricing.inputPerMillionTokens().multiply(BigDecimal.valueOf(promptTokens));
        BigDecimal output = pricing.outputPerMillionTokens().multiply(BigDecimal.valueOf(completionTokens));
        return input.add(output).divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}
