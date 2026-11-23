package com.shaahil.resumeapi.analytics;

import com.shaahil.resumeapi.ai.ChatCompleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Records every chat request. Runs on another thread so a slow or failing database can never
 * delay or break a chat answer; a failure is logged and the row is lost.
 */
@Component
public class ChatCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(ChatCompletedListener.class);

    private final ChatRequestLogRepository repository;
    private final CostEstimator costEstimator;

    public ChatCompletedListener(ChatRequestLogRepository repository, CostEstimator costEstimator) {
        this.repository = repository;
        this.costEstimator = costEstimator;
    }

    @Async
    @EventListener
    public void on(ChatCompleted event) {
        try {
            repository.save(ChatRequestLog.from(event,
                    costEstimator.estimate(event.promptTokens(), event.completionTokens())));
        } catch (RuntimeException exception) {
            log.warn("could not record chat request errorType={}", exception.getClass().getSimpleName());
        }
    }
}
