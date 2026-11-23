package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PreChatRouter router;
    private final PromptBuilder promptBuilder;
    private final AnswerStreamer answerStreamer;
    private final AiProperties properties;
    private final DailyModelCallCap dailyCap;

    public ChatService(PreChatRouter router, PromptBuilder promptBuilder, AnswerStreamer answerStreamer,
                       AiProperties properties, DailyModelCallCap dailyCap) {
        this.router = router;
        this.promptBuilder = promptBuilder;
        this.answerStreamer = answerStreamer;
        this.properties = properties;
        this.dailyCap = dailyCap;
    }

    /**
     * Never fails: any error while routing or streaming becomes a friendly fallback reply,
     * so the visitor always gets a normal event stream that ends with "done".
     */
    public Flux<ChatEvent> answer(String question) {
        return Flux.defer(() -> respond(question)).onErrorResume(this::fallback);
    }

    private Flux<ChatEvent> respond(String question) {
        PreChatResult result = router.route(question);
        SearchOutcome search = result.search();
        List<RetrievedChunk> chunks = search == null || search.chunks() == null ? List.of() : search.chunks();
        List<String> keys = chunks.stream().map(RetrievedChunk::key).toList();
        log.info("chat route={} bestScore={} kept={} keys={}", result.route(),
                search == null ? 0.0 : search.bestScore(), chunks.size(), keys);
        if (result.route() == Route.WELCOME || result.route() == Route.DECLINE_BELOW_FLOOR) {
            return Flux.just(ChatEvent.token(result.replyText()), ChatEvent.done());
        }

        if (!dailyCap.tryAcquire()) {
            log.warn("chat daily model-call cap reached");
            return Flux.just(ChatEvent.token(ReplyTemplates.dailyLimitReached()), ChatEvent.done());
        }

        Flux<ChatEvent> tokens = guardedStream(
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
    private Flux<String> guardedStream(String systemMessage, String userMessage) {
        AiProperties.Chat chat = properties.chat();
        return Flux.defer(() -> {
            AtomicBoolean started = new AtomicBoolean();
            return Flux.defer(() -> answerStreamer.stream(systemMessage, userMessage))
                    .timeout(chat.timeout())
                    .doOnNext(token -> started.set(true))
                    .retryWhen(Retry.backoff(chat.maxRetries(), chat.retryBackoff())
                            .filter(error -> !started.get())
                            .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
        });
    }

    private Flux<ChatEvent> fallback(Throwable error) {
        log.warn("chat failed errorType={}", error.getClass().getSimpleName());
        return Flux.just(ChatEvent.token(ReplyTemplates.fallback()), ChatEvent.done());
    }
}
