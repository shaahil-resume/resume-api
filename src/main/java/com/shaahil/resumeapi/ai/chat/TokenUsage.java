package com.shaahil.resumeapi.ai.chat;

/** Tokens the model reported for one request. */
public record TokenUsage(int promptTokens, int completionTokens) {
}
