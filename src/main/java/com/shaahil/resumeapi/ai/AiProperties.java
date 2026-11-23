package com.shaahil.resumeapi.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(Retrieval retrieval, Chat chat, Prompt prompt) {

    public AiProperties {
        retrieval = retrieval == null ? new Retrieval(null, null, null, null) : retrieval;
        chat = chat == null ? new Chat(null, null, null) : chat;
        prompt = prompt == null ? new Prompt(null) : prompt;
    }

    public record Retrieval(Double floor, Double threshold, Integer topK, Integer keepTopN) {

        public Retrieval {
            floor = floor == null ? 0.0 : floor;
            threshold = threshold == null ? 0.0 : threshold;
            topK = topK == null ? 10 : topK;
            keepTopN = keepTopN == null ? 5 : keepTopN;

            requireRange("ai.retrieval.floor", floor, 0.0, 1.0);
            requireRange("ai.retrieval.threshold", threshold, 0.0, 1.0);
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

    public record Chat(Double temperature, Integer maxTokens, Integer turnCap) {

        public Chat {
            temperature = temperature == null ? 0.2 : temperature;
            maxTokens = maxTokens == null ? 500 : maxTokens;
            turnCap = turnCap == null ? 10 : turnCap;

            requireRange("ai.chat.temperature", temperature, 0.0, 1.0);
            if (maxTokens < 1) {
                throw new IllegalArgumentException("ai.chat.maxTokens must be at least 1: " + maxTokens);
            }
            if (turnCap < 1) {
                throw new IllegalArgumentException("ai.chat.turnCap must be at least 1: " + turnCap);
            }
        }
    }

    public record Prompt(String version) {

        public Prompt {
            version = version == null || version.isBlank() ? "v5" : version;
        }
    }

    private static void requireRange(String property, double value, double minimum, double maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    property + " must be between " + minimum + " and " + maximum + ": " + value);
        }
    }
}
