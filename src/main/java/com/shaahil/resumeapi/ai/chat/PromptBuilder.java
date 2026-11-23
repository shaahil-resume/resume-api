package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import com.shaahil.resumeapi.ai.ingest.QuickFactsProvider;
import com.shaahil.resumeapi.ai.retrieval.RetrievedChunk;
import com.shaahil.resumeapi.ai.retrieval.SearchOutcome;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    private final QuickFactsProvider quickFactsProvider;
    private final AiProperties properties;

    public PromptBuilder(QuickFactsProvider quickFactsProvider, AiProperties properties) {
        this.quickFactsProvider = quickFactsProvider;
        this.properties = properties;
    }

    public String systemMessage(SearchOutcome search) {
        List<RetrievedChunk> chunks = search == null || search.chunks() == null ? List.of() : search.chunks();
        StringBuilder message = new StringBuilder();
        message.append("Answer recruiter questions about Shaahil.\n")
                .append("Rules:\n")
                .append("- You are the AI assistant on Shaahil's portfolio, speaking as Shaahil in the first person (\"I\", \"my\"). Convert the third-person facts in the context into first person.\n")
                .append("- Never mention \"the material\", \"the context\", \"the provided information\", \"quick facts\", \"documents\" or \"sources\". Never write source labels or citation tags such as [quick_facts] or chunk keys in the answer text.\n")
                .append("- If something is not in what you were given, say so in first person, briefly, e.g. \"I don't have that on my resume\" and point to the contact form. Do not guess, and do not invent numbers, employers, dates or titles.\n")
                .append("- If the question contains a false premise, correct it politely in first person (\"I haven't worked at Google\").\n")
                .append("- Keep answers short and conversational.\n")
                .append("- For questions about technologies, tools, skills or years of experience, answer from the Technology timeline in the quick facts. Give the years and where I used it, with \"about\" as written.\n")
                .append("- Never say \"I don't have that\" for any technology that appears in the timeline. If a technology is listed without years, say where I used it and do not state years.\n")
                .append("- If a technology is not in the timeline (for example Python, Go or Rust), say it is not on my resume and mention what I do use.\n")
                .append("- Never invent years or numbers that are not in the quick facts.\n")
                .append("- If the question states a specific figure, percentage, title, employer, date or achievement that is not in the quick facts or context, do not confirm it or repeat it as true. Say plainly that it isn't on my resume, then share the closest verified fact.\n")
                .append("- When a technology in the Technology timeline has no years listed, say where I used it and do not mention that years are missing.\n")
                .append("- When the answer is simply not in the facts (information missing, not a false premise), say so in one short first-person sentence, then offer one or two related things I can answer, and end with: \"You can also ask me directly through the contact form.\"\n")
                .append("- Never say \"I don't have a specific number of years\" (or similar) for a technology. If no years are listed, describe where and how I used it and do not mention missing numbers.\n")
                .append("Prompt version: ").append(properties.prompt().version()).append('\n')
                .append("<quick_facts>\n")
                .append(quickFactsProvider.text())
                .append("\n</quick_facts>\n");

        if (chunks.isEmpty()) {
            message.append("<context/>\n");
        } else {
            message.append("<context>\n");
            for (RetrievedChunk chunk : chunks) {
                message.append("<chunk id=\"").append(escapeAngles(chunk.key())).append("\">")
                        .append(escapeAngles(chunk.text()))
                        .append("</chunk>\n");
            }
            message.append("</context>\n");
        }
        return message.toString();
    }

    public String userMessage(String question) {
        return "<question>" + escapeAngles(question) + "</question>";
    }

    private static String escapeAngles(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<", "&lt;").replace(">", "&gt;");
    }
}
