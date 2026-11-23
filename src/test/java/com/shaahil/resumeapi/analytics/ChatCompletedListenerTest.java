package com.shaahil.resumeapi.analytics;

import com.shaahil.resumeapi.ai.ChatCompleted;
import com.shaahil.resumeapi.ai.ChatOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatCompletedListenerTest {

    private final ChatRequestLogRepository repository = mock(ChatRequestLogRepository.class);
    private final ChatCompletedListener listener = new ChatCompletedListener(repository,
            new CostEstimator(new AnalyticsPricingProperties(null, null)));

    private static ChatCompleted event(ChatOutcome outcome) {
        return new ChatCompleted(Instant.parse("2026-11-24T01:00:00Z"), outcome, 0.42, 5, 1250, 1, "v5", 2000, 500);
    }

    @Test
    void savesEveryFieldOfTheEvent() {
        listener.on(event(ChatOutcome.ANSWERED));

        ArgumentCaptor<ChatRequestLog> saved = ArgumentCaptor.forClass(ChatRequestLog.class);
        verify(repository).save(saved.capture());
        ChatRequestLog row = saved.getValue();
        assertThat(row.getOccurredAt()).isEqualTo(Instant.parse("2026-11-24T01:00:00Z"));
        assertThat(row.getOutcome()).isEqualTo(ChatOutcome.ANSWERED);
        assertThat(row.getBestScore()).isEqualTo(0.42);
        assertThat(row.getChunksKept()).isEqualTo(5);
        assertThat(row.getLatencyMs()).isEqualTo(1250);
        assertThat(row.getRetries()).isEqualTo(1);
        assertThat(row.getPromptVersion()).isEqualTo("v5");
        assertThat(row.getPromptTokens()).isEqualTo(2000);
        assertThat(row.getCompletionTokens()).isEqualTo(500);
        // (2000 x 0.15 + 500 x 0.60) / 1,000,000
        assertThat(row.getEstimatedCostUsd()).isEqualByComparingTo("0.000600");
    }

    @Test
    void aDatabaseFailureIsSwallowedSoChatIsNeverAffected() {
        when(repository.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatCode(() -> listener.on(event(ChatOutcome.FALLBACK))).doesNotThrowAnyException();
    }

    @Test
    void costIsLeftEmptyWhenTheProviderReportedNoTokens() {
        listener.on(new ChatCompleted(Instant.parse("2026-11-24T01:00:00Z"), ChatOutcome.DECLINED,
                0.05, 0, 40, 0, "v5", null, null));

        ArgumentCaptor<ChatRequestLog> saved = ArgumentCaptor.forClass(ChatRequestLog.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getPromptTokens()).isNull();
        assertThat(saved.getValue().getEstimatedCostUsd()).isNull();
    }
}
