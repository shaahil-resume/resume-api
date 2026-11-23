package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.limits.VisitorAddress;
import com.shaahil.resumeapi.ai.limits.VisitorRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private final ChatService chatService;
    private final VisitorRateLimiter rateLimiter;
    private final ChatEventPublisher chatEvents;

    public ChatController(ChatService chatService, VisitorRateLimiter rateLimiter,
                          ChatEventPublisher chatEvents) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
        this.chatEvents = chatEvents;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody ChatRequest request,
                                              HttpServletRequest http) {
        VisitorRateLimiter.Decision decision = rateLimiter.check(VisitorAddress.of(http));
        if (!decision.allowed()) {
            chatEvents.rateLimited();
            throw new TooManyQuestionsException(decision.retryAfterSeconds());
        }
        return chatService.answer(request.question())
                .map(event -> ServerSentEvent.builder(event.data())
                        .event(event.type())
                        .build());
    }

    @ExceptionHandler(TooManyQuestionsException.class)
    ResponseEntity<Void> tooManyQuestions(TooManyQuestionsException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
                .build();
    }

    static final class TooManyQuestionsException extends RuntimeException {

        private final long retryAfterSeconds;

        TooManyQuestionsException(long retryAfterSeconds) {
            super("rate limit exceeded", null, false, false);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    public record ChatRequest(@NotBlank @Size(max = 500) String question) {
    }
}
