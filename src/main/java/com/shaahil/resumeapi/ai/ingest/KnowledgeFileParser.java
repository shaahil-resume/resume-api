package com.shaahil.resumeapi.ai.ingest;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class KnowledgeFileParser {

    private static final String FRONT_MATTER_DELIMITER = "---";

    private final Yaml yaml = new Yaml();
    private final PathMatchingResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    public KnowledgeDocument parse(String fileContent) {
        return parse(fileContent, "file content");
    }

    public List<KnowledgeDocument> loadAll(String directoryPattern) {
        try {
            Resource[] resources = resourceResolver.getResources(directoryPattern);
            List<KnowledgeDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || filename.equals("quick-facts.md") || filename.equals("README.md")) {
                    continue;
                }
                String fileContent = resource.getContentAsString(StandardCharsets.UTF_8);
                documents.add(parse(fileContent, filename));
            }
            return documents.stream()
                    .sorted(Comparator.comparing(document -> document.section() + "\u0000" + document.role()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not load knowledge files from " + directoryPattern, exception);
        }
    }

    private KnowledgeDocument parse(String fileContent, String filename) {
        String[] lines = fileContent.split("\\R", -1);
        if (lines.length == 0 || !FRONT_MATTER_DELIMITER.equals(lines[0].trim())) {
            throw new IllegalArgumentException("Knowledge file " + filename + " is missing front matter");
        }

        int closingDelimiter = -1;
        for (int index = 1; index < lines.length; index++) {
            if (FRONT_MATTER_DELIMITER.equals(lines[index].trim())) {
                closingDelimiter = index;
                break;
            }
        }
        if (closingDelimiter < 0) {
            throw new IllegalArgumentException("Knowledge file " + filename + " is missing front matter");
        }

        StringBuilder frontMatter = new StringBuilder();
        for (int index = 1; index < closingDelimiter; index++) {
            frontMatter.append(lines[index]).append('\n');
        }
        Map<String, Object> metadata = yaml.load(frontMatter.toString());
        if (metadata == null || metadata.get("section") == null
                || metadata.get("section").toString().isBlank()) {
            throw new IllegalArgumentException("Knowledge file " + filename + " is missing section");
        }

        List<String> skills = metadata.get("skills") instanceof List<?> values
                ? values.stream().map(String::valueOf).toList()
                : List.of();
        String body = String.join("\n", List.of(lines).subList(closingDelimiter + 1, lines.length)).trim();

        return new KnowledgeDocument(
                value(metadata, "section"),
                value(metadata, "company"),
                value(metadata, "role"),
                value(metadata, "startDate"),
                value(metadata, "endDate"),
                skills,
                value(metadata, "docVersion"),
                body
        );
    }

    private static String value(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }
}
