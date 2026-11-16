package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import com.shaahil.resumeapi.ai.retrieval.SearchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PreChatRouterTest {

    @Test
    void greetingsAndThanksDoNotSearch() {
        AtomicInteger searches = new AtomicInteger();
        SearchService searchService = handWrittenSearchService(searches, new SearchOutcome(false, 0.0, List.of()));
        PreChatRouter router = new PreChatRouter(searchService);

        for (String greeting : List.of("hi", "Hello!", "HEY", " hi there ", "Good morning.",
                "good afternoon", "good evening!", "thanks", "thank you!", "thx")) {
            PreChatResult result = router.route(greeting);
            assertThat(result.route()).isEqualTo(Route.WELCOME);
            assertThat(result.replyText()).isEqualTo(ReplyTemplates.welcome());
            assertThat(result.search()).isNull();
        }

        assertThat(searches).hasValue(0);
    }

    @Test
    void belowFloorDeclinesWithSearchOutcome() {
        SearchOutcome outcome = new SearchOutcome(false, 0.2, List.of());
        PreChatRouter router = new PreChatRouter(handWrittenSearchService(new AtomicInteger(), outcome));

        PreChatResult result = router.route("Tell me about something else");

        assertThat(result.route()).isEqualTo(Route.DECLINE_BELOW_FLOOR);
        assertThat(result.replyText()).isEqualTo(ReplyTemplates.declineBelowFloor());
        assertThat(result.search()).isSameAs(outcome);
    }

    @Test
    void aboveFloorRoutesToModelWithChunks() {
        SearchOutcome outcome = new SearchOutcome(true, 0.9,
                List.of(new RetrievedChunk("id", "experience:acme:engineer:0", "text", 0.9, Map.of())));
        PreChatResult result = new PreChatRouter(
                handWrittenSearchService(new AtomicInteger(), outcome)).route("What has he built?");

        assertThat(result.route()).isEqualTo(Route.ANSWER_WITH_MODEL);
        assertThat(result.replyText()).isNull();
        assertThat(result.search()).isSameAs(outcome);
    }

    @Test
    void blankQuestionThrows() {
        PreChatRouter router = new PreChatRouter(
                handWrittenSearchService(new AtomicInteger(), new SearchOutcome(false, 0.0, List.of())));

        assertThatIllegalArgumentException().isThrownBy(() -> router.route("  "));
    }

    private static SearchService handWrittenSearchService(AtomicInteger searches, SearchOutcome outcome) {
        AiProperties properties = new AiProperties(
                new AiProperties.Retrieval(0.0, 0.0, 10, 5), null, null);
        return new SearchService(null, properties) {
            @Override
            public SearchOutcome search(String question) {
                searches.incrementAndGet();
                return outcome;
            }
        };
    }
}
