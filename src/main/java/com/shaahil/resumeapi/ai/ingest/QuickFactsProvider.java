package com.shaahil.resumeapi.ai.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class QuickFactsProvider {

    private final String text;

    public QuickFactsProvider(@Value("classpath:knowledge/quick-facts.md") Resource resource) {
        this.text = readText(resource);
    }

    public String text() {
        return text;
    }

    private static String readText(Resource resource) {
        final String content;
        try {
            content = resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read quick-facts file " + resource.getDescription(), exception);
        }
        String body = stripFrontMatter(content).trim();
        if (body.isBlank()) {
            throw new IllegalStateException("Quick-facts file " + resource.getDescription() + " is blank");
        }
        return body;
    }

    private static String stripFrontMatter(String content) {
        String[] lines = content.split("\\R", -1);
        if (lines.length == 0 || !"---".equals(lines[0].trim())) {
            return content;
        }
        for (int index = 1; index < lines.length; index++) {
            if ("---".equals(lines[index].trim())) {
                return String.join("\n", List.of(lines).subList(index + 1, lines.length));
            }
        }
        return content;
    }
}
