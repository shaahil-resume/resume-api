package com.shaahil.resumeapi.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CostEstimatorTest {

    private final CostEstimator estimator = new CostEstimator(
            new AnalyticsPricingProperties(new BigDecimal("0.15"), new BigDecimal("0.60")));

    @Test
    void estimatesFromInputAndOutputPrices() {
        // (1000 x 0.15 + 500 x 0.60) / 1,000,000 = 0.00045
        assertThat(estimator.estimate(1000, 500)).isEqualByComparingTo("0.000450");
    }

    @Test
    void zeroTokensCostNothing() {
        assertThat(estimator.estimate(0, 0)).isEqualByComparingTo("0");
    }

    @Test
    void unknownTokensGiveNoEstimate() {
        assertThat(estimator.estimate(null, 500)).isNull();
        assertThat(estimator.estimate(1000, null)).isNull();
    }

    @Test
    void defaultsAreTheGpt4oMiniListPrices() {
        AnalyticsPricingProperties defaults = new AnalyticsPricingProperties(null, null);

        assertThat(defaults.inputPerMillionTokens()).isEqualByComparingTo("0.15");
        assertThat(defaults.outputPerMillionTokens()).isEqualByComparingTo("0.60");
    }

    @Test
    void negativePricesAreRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnalyticsPricingProperties(new BigDecimal("-1"), null))
                .withMessageContaining("analytics.pricing.inputPerMillionTokens");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AnalyticsPricingProperties(null, new BigDecimal("-1")))
                .withMessageContaining("analytics.pricing.outputPerMillionTokens");
    }
}
