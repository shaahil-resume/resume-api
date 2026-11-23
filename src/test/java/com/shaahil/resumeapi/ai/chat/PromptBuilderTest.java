package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.ingest.QuickFactsProvider;
import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void systemAndUserMessagesKeepStablePrefixBeforeQuestion() {
        PromptBuilder builder = new PromptBuilder(
                new QuickFactsProvider(new ByteArrayResource("Shaahil works with Java.".getBytes(StandardCharsets.UTF_8))),
                new AiProperties(null, null, new AiProperties.Prompt("v-test")));
        SearchOutcome outcome = new SearchOutcome(true, 0.9,
                List.of(new RetrievedChunk("id", "experience:wipro:engineer:0", "Uses <Spring>.", 0.9, Map.of())));

        String system = builder.systemMessage(outcome);
        String user = builder.userMessage("What <did> he build?");

        assertThat(system.indexOf("Rules:")).isLessThan(system.indexOf("<quick_facts>"));
        assertThat(system.indexOf("<quick_facts>")).isLessThan(system.indexOf("<context>"));
        assertThat(system).contains("Prompt version: v-test")
                .contains("You are the AI assistant on Shaahil's portfolio, speaking as Shaahil in the first person")
                .contains("Never mention \"the material\"")
                .contains("Never write source labels or citation tags")
                .contains("<chunk id=\"experience:wipro:engineer:0\">Uses &lt;Spring&gt;.</chunk>");
        assertThat(user).isEqualTo("<question>What &lt;did&gt; he build?</question>");
    }

    @Test
    void emptyContextIsExplicitAndQuestionIsLastMessage() {
        PromptBuilder builder = new PromptBuilder(
                new QuickFactsProvider(new ByteArrayResource("facts".getBytes(StandardCharsets.UTF_8))),
                new AiProperties(null, null, null));

        String system = builder.systemMessage(new SearchOutcome(false, 0.0, List.of()));
        String user = builder.userMessage("Thanks");

        assertThat(system).contains("Prompt version: v5")
                .contains("For questions about technologies, tools, skills or years of experience, answer from the Technology timeline")
                .contains("Never say \"I don't have that\" for any technology that appears in the timeline")
                .contains("If a technology is not in the timeline")
                .contains("Never invent years or numbers that are not in the quick facts")
                .contains("If the question states a specific figure, percentage, title, employer, date or achievement that is not in the quick facts or context")
                .contains("When a technology in the Technology timeline has no years listed, say where I used it and do not mention that years are missing")
                .contains("When the answer is simply not in the facts (information missing, not a false premise), say so in one short first-person sentence")
                .contains("You can also ask me directly through the contact form.")
                .contains("Never say \"I don't have a specific number of years\"")
                .contains("<context/>")
                .doesNotContain("<chunk");
        assertThat(system).endsWith("<context/>\n");
        assertThat(user).isEqualTo("<question>Thanks</question>");
    }
}
