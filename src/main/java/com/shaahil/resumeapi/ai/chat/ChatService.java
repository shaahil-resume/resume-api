package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PreChatRouter router;
    private final PromptBuilder promptBuilder;
    private final AnswerStreamer answerStreamer;

    public ChatService(PreChatRouter router, PromptBuilder promptBuilder, AnswerStreamer answerStreamer) {
        this.router = router;
        this.promptBuilder = promptBuilder;
        this.answerStreamer = answerStreamer;
    }

    public Flux<ChatEvent> answer(String question) {
        PreChatResult result = router.route(question);
        SearchOutcome search = result.search();
        int kept = search == null || search.chunks() == null ? 0 : search.chunks().size();
        String keys = search == null || search.chunks() == null
                ? "[]"
                : search.chunks().stream()
                .map(RetrievedChunk::key)
                .toList()
                .toString();
        log.info("chat route={} bestScore={} kept={} keys={}", result.route(),
                search == null ? 0.0 : search.bestScore(), kept, keys);
        if (result.route() == Route.WELCOME || result.route() == Route.DECLINE_BELOW_FLOOR) {
            return Flux.just(ChatEvent.token(result.replyText()), ChatEvent.done());
        }

        Flux<ChatEvent> tokens = answerStreamer.stream(
                        promptBuilder.systemMessage(search), promptBuilder.userMessage(question))
                .map(ChatEvent::token);
        String citations = search.chunks().stream()
                .map(RetrievedChunk::key)
                .filter(key -> key != null)
                .collect(Collectors.joining(","));
        return tokens.concatWith(Flux.just(ChatEvent.citations(citations), ChatEvent.done()));
    }
}
