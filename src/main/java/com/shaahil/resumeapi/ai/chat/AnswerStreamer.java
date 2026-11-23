package com.shaahil.resumeapi.ai.chat;

import reactor.core.publisher.Flux;

public interface AnswerStreamer {

    Flux<String> stream(String systemMessage, String userMessage);
}
