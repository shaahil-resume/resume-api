package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.ChatOutcome;
import com.shaahil.resumeapi.ai.limits.DailyModelCallCap;
import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PreChatRouter router;
    private final PromptBuilder promptBuilder;
    private final AnswerStreamer answerStreamer;
    private final AiProperties properties;
    private final DailyModelCallCap dailyCap;
    private final ChatEventPublisher chatEvents;

    public ChatService(PreChatRouter router, PromptBuilder promptBuilder, AnswerStreamer answerStreamer,
                       AiProperties properties, DailyModelCallCap dailyCap, ChatEventPublisher chatEvents) {
        this.router = router;
        this.promptBuilder = promptBuilder;
        this.answerStreamer = answerStreamer;
        this.properties = properties;
        this.dailyCap = dailyCap;
        this.chatEvents = chatEvents;
    }

    /**
     * Never fails: any error while routing or streaming becomes a friendly fallback reply,
     * so the visitor always gets a normal event stream that ends with "done".
     */
    public Flux<ChatEvent> answer(String question) {
        return Flux.defer(() -> {
            RequestStats stats = new RequestStats();
            return Flux.defer(() -> respond(question, stats))
                    .onErrorResume(error -> fallback(error, stats))
                    .doOnComplete(() -> publish(stats))
                    .doOnCancel(() -> publish(stats));
        });
    }

    private Flux<ChatEvent> respond(String question, RequestStats stats) {
        PreChatResult result = router.route(question);
        SearchOutcome search = result.search();
        List<RetrievedChunk> chunks = search == null || search.chunks() == null ? List.of() : search.chunks();
        List<String> keys = chunks.stream().map(RetrievedChunk::key).toList();
        stats.bestScore = search == null ? 0.0 : search.bestScore();
        stats.chunksKept = chunks.size();
        log.info("chat route={} bestScore={} kept={} keys={}", result.route(),
                stats.bestScore, chunks.size(), keys);
        if (result.route() == Route.WELCOME || result.route() == Route.DECLINE_BELOW_FLOOR) {
            stats.outcome = result.route() == Route.WELCOME ? ChatOutcome.WELCOME : ChatOutcome.DECLINED;
            return Flux.just(ChatEvent.token(result.replyText()), ChatEvent.done());
        }

        if (!dailyCap.tryAcquire()) {
            log.warn("chat daily model-call cap reached");
            stats.outcome = ChatOutcome.DAILY_CAP;
            return Flux.just(ChatEvent.token(ReplyTemplates.dailyLimitReached()), ChatEvent.done());
        }

        stats.outcome = ChatOutcome.ANSWERED;
        Flux<ChatEvent> tokens = guardedStream(stats,
                        promptBuilder.systemMessage(search), promptBuilder.userMessage(question))
                .map(ChatEvent::token);
        String citations = keys.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(","));
        return tokens.concatWith(Flux.just(ChatEvent.citations(citations), ChatEvent.done()));
    }

    /**
     * A silent model call is cut off after the configured timeout. A failure before the first
     * token is retried (a retry after text has started would repeat that text). The inner defer
     * makes every retry start a fresh model call instead of re-subscribing to the same Flux.
     */
    private Flux<String> guardedStream(RequestStats stats, String systemMessage, String userMessage) {
        AiProperties.Chat chat = properties.chat();
        return Flux.defer(() -> {
            AtomicBoolean started = new AtomicBoolean();
            return Flux.defer(() -> {
                        stats.attempts.incrementAndGet();
                        return answerStreamer.stream(systemMessage, userMessage, usage -> stats.usage = usage);
                    })
                    .timeout(chat.timeout())
                    .doOnNext(token -> started.set(true))
                    .retryWhen(Retry.backoff(chat.maxRetries(), chat.retryBackoff())
                            .filter(error -> !started.get())
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
        });
    }

    private Flux<ChatEvent> fallback(Throwable error, RequestStats stats) {
        log.warn("chat failed errorType={}", error.getClass().getSimpleName());
        stats.outcome = ChatOutcome.FALLBACK;
        return Flux.just(ChatEvent.token(ReplyTemplates.fallback()), ChatEvent.done());
    }

    /**
     * Published when the stream completes (before the last signal reaches the caller) or when the
     * visitor disconnects. A visitor who leaves mid-answer still cost a model call, so that counts
     * as answered. Nothing is published before the route is known.
     */
    private void publish(RequestStats stats) {
        ChatOutcome outcome = stats.outcome;
        if (outcome == null) {
            return;
        }
        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stats.startNanos);
        chatEvents.publish(outcome, stats.bestScore, stats.chunksKept, latencyMs,
                Math.max(0, stats.attempts.get() - 1), stats.usage);
    }

    /** What happened during one request; written by the thread running the stream. */
    private static final class RequestStats {

        private final long startNanos = System.nanoTime();
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile ChatOutcome outcome;
        private volatile double bestScore;
        private volatile int chunksKept;
        private volatile TokenUsage usage;
    }
}
