package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.ChatCompleted;
import com.shaahil.resumeapi.ai.ChatOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Publishes one {@link ChatCompleted} per chat request. Publishing can never fail a chat. */
@Component
public class ChatEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ChatEventPublisher.class);

    private final ApplicationEventPublisher publisher;
    private final AiProperties properties;
    private final Clock clock;

    public ChatEventPublisher(ApplicationEventPublisher publisher, AiProperties properties, Clock clock) {
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    public void publish(ChatOutcome outcome, double bestScore, int chunksKept, long latencyMs, int retries) {
        try {
            publisher.publishEvent(new ChatCompleted(clock.instant(), outcome, bestScore, chunksKept,
                    latencyMs, retries, properties.prompt().version()));
        } catch (RuntimeException exception) {
            log.warn("could not publish chat event errorType={}", exception.getClass().getSimpleName());
        }
    }

    /** A rate-limited request never reaches the chat service, so the controller reports it. */
    public void rateLimited() {
        publish(ChatOutcome.RATE_LIMITED, 0.0, 0, 0, 0);
    }
}
