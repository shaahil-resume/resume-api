package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.ingest.QuickFactsProvider;
import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import com.shaahil.resumeapi.ai.retrieval.SearchService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class ChatServiceTest {

    @Test
    void welcomeAndDeclineDoNotCallStreamer() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            return Flux.just("unexpected");
        };
        ChatService service = service(new SearchOutcome(false, 0.1, List.of()), streamer);

        List<ChatEvent> events = service.answer("hi").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "done");
        assertThat(calls).hasValue(0);

        List<ChatEvent> declined = service(new SearchOutcome(false, 0.1, List.of()), streamer)
                .answer("something unrelated").collectList().block();
        assertThat(declined).extracting(ChatEvent::type).containsExactly("token", "done");
        assertThat(calls).hasValue(0);
    }

    @Test
    void modelAnswerStreamsTokensThenCitationsAndDone() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            assertThat(system).contains("<context>");
            assertThat(user).contains("<question>What did he build?</question>");
            return Flux.just("He ", "built it.");
        };
        SearchOutcome outcome = new SearchOutcome(true, 0.9, List.of(
                new RetrievedChunk("one", "experience:wipro:engineer:0", "one", 0.9, Map.of()),
                new RetrievedChunk("two", "project:resume:none:0", "two", 0.8, Map.of())));
        ChatService service = service(outcome, streamer);

        List<ChatEvent> events = service.answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "token", "citations", "done");
        assertThat(events).extracting(ChatEvent::data)
                .containsExactly("He ", "built it.", "experience:wipro:engineer:0,project:resume:none:0", "");
        assertThat(calls).hasValue(1);
    }

    @Test
    void logsRouteMetadataForEveryRouteWithoutQuestionText() {
        Logger logger = (Logger) LoggerFactory.getLogger(ChatService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String visitorText = "visitor question must not be logged";
        try {
            service(new SearchOutcome(false, 0.1, List.of()), (system, user) -> Flux.empty())
                    .answer("hi").collectList().block();
            service(new SearchOutcome(false, 0.1, List.of()), (system, user) -> Flux.empty())
                    .answer(visitorText).collectList().block();
            service(new SearchOutcome(true, 0.62, List.of(
                            new RetrievedChunk("id", "experience:wipro:engineer:0", "text", 0.62, Map.of()),
                            new RetrievedChunk("id2", "project:resume:none:0", "text", 0.5, Map.of()),
                            new RetrievedChunk("id3", "skills:none:none:0", "text", 0.4, Map.of()))),
                    (system, user) -> Flux.just("answer"))
                    .answer("another " + visitorText).collectList().block();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        List<String> messages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
        assertThat(messages).hasSize(3)
                .anySatisfy(message -> assertThat(message)
                        .contains("chat route=WELCOME bestScore=0.0 kept=0 keys=[]"))
                .anySatisfy(message -> assertThat(message)
                        .contains("chat route=DECLINE_BELOW_FLOOR bestScore=0.1 kept=0 keys=[]"))
                .anySatisfy(message -> assertThat(message)
                        .contains("chat route=ANSWER_WITH_MODEL bestScore=0.62 kept=3 keys=[experience:wipro:engineer:0, project:resume:none:0, skills:none:none:0]"));
        assertThat(messages).allSatisfy(message -> assertThat(message).doesNotContain(visitorText));
    }

    @Test
    void streamerFailureBecomesFallbackReplyAfterRetries() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            return Flux.error(new IllegalStateException("boom"));
        };

        List<ChatEvent> events = service(answerOutcome(), streamer, fastChat(2, Duration.ofSeconds(5)))
                .answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "done");
        assertThat(events).first().extracting(ChatEvent::data).isEqualTo(ReplyTemplates.fallback());
        assertThat(calls).hasValue(3);
    }

    @Test
    void transientFailureBeforeFirstTokenIsRetried() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> calls.incrementAndGet() == 1
                ? Flux.error(new IllegalStateException("boom"))
                : Flux.just("ok");

        List<ChatEvent> events = service(answerOutcome(), streamer, fastChat(2, Duration.ofSeconds(5)))
                .answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "citations", "done");
        assertThat(events).first().extracting(ChatEvent::data).isEqualTo("ok");
        assertThat(calls).hasValue(2);
    }

    @Test
    void failureAfterFirstTokenIsNotRetried() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            return Flux.concat(Flux.just("part"), Flux.error(new IllegalStateException("boom")));
        };

        List<ChatEvent> events = service(answerOutcome(), streamer, fastChat(2, Duration.ofSeconds(5)))
                .answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "token", "done");
        assertThat(events).extracting(ChatEvent::data).containsExactly("part", ReplyTemplates.fallback(), "");
        assertThat(calls).hasValue(1);
    }

    @Test
    void silentModelTimesOutIntoFallbackReply() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            return Flux.never();
        };

        List<ChatEvent> events = service(answerOutcome(), streamer, fastChat(1, Duration.ofMillis(100)))
                .answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "done");
        assertThat(events).first().extracting(ChatEvent::data).isEqualTo(ReplyTemplates.fallback());
        assertThat(calls).hasValue(2);
    }

    @Test
    void routerFailureBecomesFallbackReplyWithoutCallingTheModel() {
        AtomicInteger calls = new AtomicInteger();
        AnswerStreamer streamer = (system, user) -> {
            calls.incrementAndGet();
            return Flux.just("unexpected");
        };
        SearchService failing = new SearchService(null,
                new AiProperties(new AiProperties.Retrieval(0.0, 0.0, 10, 5), null, null)) {
            @Override
            public SearchOutcome search(String question) {
                throw new IllegalStateException("embedding service down");
            }
        };

        List<ChatEvent> events = service(failing, streamer, fastChat(2, Duration.ofSeconds(5)))
                .answer("What did he build?").collectList().block();

        assertThat(events).extracting(ChatEvent::type).containsExactly("token", "done");
        assertThat(events).first().extracting(ChatEvent::data).isEqualTo(ReplyTemplates.fallback());
        assertThat(calls).hasValue(0);
    }

    private static SearchOutcome answerOutcome() {
        return new SearchOutcome(true, 0.9, List.of(
                new RetrievedChunk("one", "experience:wipro:engineer:0", "one", 0.9, Map.of())));
    }

    private static AiProperties.Chat fastChat(int maxRetries, Duration timeout) {
        return new AiProperties.Chat(0.2, 500, 10, timeout, maxRetries, Duration.ofMillis(1));
    }

    private static ChatService service(SearchOutcome outcome, AnswerStreamer streamer) {
        return service(outcome, streamer, null);
    }

    private static ChatService service(SearchOutcome outcome, AnswerStreamer streamer, AiProperties.Chat chat) {
        SearchService searchService = new SearchService(null,
                new AiProperties(new AiProperties.Retrieval(0.0, 0.0, 10, 5), null, null)) {
            @Override
            public SearchOutcome search(String question) {
                return outcome;
            }
        };
        return service(searchService, streamer, chat);
    }

    private static ChatService service(SearchService searchService, AnswerStreamer streamer,
                                       AiProperties.Chat chat) {
        PreChatRouter router = new PreChatRouter(searchService);
        PromptBuilder promptBuilder = new PromptBuilder(
                new QuickFactsProvider(new ByteArrayResource("facts".getBytes(StandardCharsets.UTF_8))),
                new AiProperties(null, null, null));
        return new ChatService(router, promptBuilder, streamer, new AiProperties(null, chat, null));
    }
}
