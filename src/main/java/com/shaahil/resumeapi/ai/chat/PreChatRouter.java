package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import com.shaahil.resumeapi.ai.retrieval.SearchService;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class PreChatRouter {

    private static final Set<String> GREETINGS = Set.of(
            "hi", "hello", "hey", "hi there", "good morning", "good afternoon", "good evening",
            "thanks", "thank you", "thx");

    private final SearchService searchService;

    public PreChatRouter(SearchService searchService) {
        this.searchService = searchService;
    }

    public PreChatResult route(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank");
        }
        if (GREETINGS.contains(normalise(question))) {
            return new PreChatResult(Route.WELCOME, ReplyTemplates.welcome(), null);
        }

        SearchOutcome outcome = searchService.search(question);
        if (!outcome.aboveFloor()) {
            return new PreChatResult(Route.DECLINE_BELOW_FLOOR, ReplyTemplates.declineBelowFloor(), outcome);
        }
        return new PreChatResult(Route.ANSWER_WITH_MODEL, null, outcome);
    }

    private static String normalise(String question) {
        return question.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}]", "")
                .replaceAll("\\s+", " ");
    }
}
