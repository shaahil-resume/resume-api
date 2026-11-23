package com.shaahil.resumeapi.ai.chat;

import reactor.core.publisher.Flux;

import java.util.function.Consumer;

public interface AnswerStreamer {

    Flux<String> stream(String systemMessage, String userMessage);

    /**
     * Same as {@link #stream(String, String)}, and hands the token usage to {@code usageSink} when
     * the provider reports it (it arrives near the end of the stream). The default implementation
     * never reports usage.
     */
    default Flux<String> stream(String systemMessage, String userMessage, Consumer<TokenUsage> usageSink) {
        return stream(systemMessage, userMessage);
    }
}
