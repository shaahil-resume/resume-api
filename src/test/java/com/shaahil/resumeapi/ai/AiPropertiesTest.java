package com.shaahil.resumeapi.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AiPropertiesTest {

    @Test
    void defaultsApplyWhenNothingIsSet() {
        AiProperties properties = new AiProperties(null, null, null);

        assertThat(properties.retrieval()).isEqualTo(new AiProperties.Retrieval(0.0, 0.0, 10, 5));
        assertThat(properties.chat()).isEqualTo(new AiProperties.Chat(0.2, 500, 10));
        assertThat(properties.prompt()).isEqualTo(new AiProperties.Prompt("v1"));
    }

    @Test
    void floorHigherThanThresholdIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiProperties.Retrieval(0.6, 0.4, 10, 5))
                .withMessageContaining("floor");
    }

    @Test
    void keepTopNHigherThanTopKIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiProperties.Retrieval(0.1, 0.4, 5, 8))
                .withMessageContaining("keepTopN");
    }

    @Test
    void thresholdAboveOneIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiProperties.Retrieval(0.0, 1.1, 10, 5))
                .withMessageContaining("ai.retrieval.threshold");
    }

    @Test
    void temperatureAboveOneIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiProperties.Chat(1.1, 500, 10))
                .withMessageContaining("ai.chat.temperature");
    }

    @Test
    void zeroTurnCapIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new AiProperties.Chat(0.2, 500, 0))
                .withMessageContaining("ai.chat.turnCap");
    }
}
