package com.shaahil.resumeapi.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(Retrieval retrieval, Chat chat, Prompt prompt) {

    public AiProperties {
        retrieval = retrieval == null ? new Retrieval(null, null, null, null) : retrieval;
        chat = chat == null ? new Chat(null, null, null, null, null, null) : chat;
        prompt = prompt == null ? new Prompt(null) : prompt;
    }

    public record Retrieval(Double floor, Double threshold, Integer topK, Integer keepTopN) {

        public Retrieval {
            floor = floor == null ? 0.0 : floor;
            threshold = threshold == null ? 0.0 : threshold;
            topK = topK == null ? 10 : topK;
            keepTopN = keepTopN == null ? 5 : keepTopN;

            requireFraction("ai.retrieval.floor", floor);
            requireFraction("ai.retrieval.threshold", threshold);
            if (floor > threshold) {
                throw new IllegalArgumentException(
                        "ai.retrieval.floor must not be higher than ai.retrieval.threshold: " + floor + " > " + threshold);
            }
            if (topK < 1) {
                throw new IllegalArgumentException("ai.retrieval.topK must be at least 1: " + topK);
            }
            if (keepTopN < 1 || keepTopN > topK) {
                throw new IllegalArgumentException(
                        "ai.retrieval.keepTopN must be between 1 and ai.retrieval.topK: " + keepTopN);
            }
        }
    }

    public record Chat(Double temperature, Integer maxTokens, Integer turnCap,
                       Duration timeout, Integer maxRetries, Duration retryBackoff) {

        public Chat {
            temperature = temperature == null ? 0.2 : temperature;
            maxTokens = maxTokens == null ? 500 : maxTokens;
            turnCap = turnCap == null ? 10 : turnCap;
            timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
            maxRetries = maxRetries == null ? 2 : maxRetries;
            retryBackoff = retryBackoff == null ? Duration.ofMillis(300) : retryBackoff;

            requireFraction("ai.chat.temperature", temperature);
            if (maxTokens < 1) {
                throw new IllegalArgumentException("ai.chat.maxTokens must be at least 1: " + maxTokens);
            }
            if (turnCap < 1) {
                throw new IllegalArgumentException("ai.chat.turnCap must be at least 1: " + turnCap);
            }
            if (timeout.isZero() || timeout.isNegative()) {
                throw new IllegalArgumentException("ai.chat.timeout must be positive: " + timeout);
            }
            if (maxRetries < 0 || maxRetries > 5) {
                throw new IllegalArgumentException("ai.chat.maxRetries must be between 0 and 5: " + maxRetries);
            }
            if (retryBackoff.isNegative()) {
                throw new IllegalArgumentException("ai.chat.retryBackoff must not be negative: " + retryBackoff);
            }
        }
    }

    public record Prompt(String version) {

        public Prompt {
            version = version == null || version.isBlank() ? "v5" : version;
        }
    }

    private static void requireFraction(String property, double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(property + " must be between 0.0 and 1.0: " + value);
        }
    }
}
