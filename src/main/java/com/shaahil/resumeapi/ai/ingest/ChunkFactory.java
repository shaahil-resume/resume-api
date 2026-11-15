package com.shaahil.resumeapi.ai.ingest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public final class ChunkFactory {

    private ChunkFactory() {
    }

    public static String slug(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String slug = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "none" : slug;
    }

    public static String key(KnowledgeDocument doc, int index) {
        return String.join(":", slug(doc.section()), slug(doc.company()), slug(doc.role()), String.valueOf(index));
    }

    public static UUID id(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    public static String header(KnowledgeDocument doc) {
        String role = nonBlank(doc.role()) ? doc.role().trim() : null;
        String company = nonBlank(doc.company()) ? "at " + doc.company().trim() : null;
        String dates = null;
        if (nonBlank(doc.startDate())) {
            dates = doc.startDate().trim() + " to "
                    + (nonBlank(doc.endDate()) ? doc.endDate().trim() : "present");
        }

        String header = Stream.of(role, company, dates)
                .filter(value -> value != null)
                .reduce((left, right) -> left + " " + right)
                .orElseGet(() -> capitaliseFirst(slug(doc.section())));
        return header + ":";
    }

    public static String text(KnowledgeDocument doc) {
        return header(doc) + "\n\n" + (doc.body() == null ? "" : doc.body().trim());
    }

    public static String hash(KnowledgeDocument doc) {
        return hash(doc, "text-embedding-3-small");
    }

    public static String hash(KnowledgeDocument doc, String embeddingModel) {
        String hashInput = text(doc) + "\n"
                + value(doc.section()) + "|"
                + value(doc.company()) + "|"
                + value(doc.role()) + "|"
                + value(doc.startDate()) + "|"
                + value(doc.endDate()) + "|"
                + String.join(",", doc.skills() == null
                ? List.of()
                : doc.skills().stream().map(ChunkFactory::value).toList()) + "|"
                + value(embeddingModel);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(hashInput.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hexadecimal.append(String.format("%02x", value));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public static Chunk build(KnowledgeDocument doc, int index) {
        return build(doc, index, "text-embedding-3-small", 1536);
    }

    public static Chunk build(KnowledgeDocument doc, int index, String embeddingModel, int embeddingDims) {
        String key = key(doc, index);
        UUID id = id(key);
        String text = text(doc);
        String contentHash = hash(doc, embeddingModel);
        Map<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "section", doc.section());
        putIfPresent(metadata, "company", doc.company());
        putIfPresent(metadata, "role", doc.role());
        putIfPresent(metadata, "startDate", doc.startDate());
        putIfPresent(metadata, "endDate", doc.endDate());
        if (doc.skills() != null && !doc.skills().isEmpty()) {
            metadata.put("skills", doc.skills());
        }
        putIfPresent(metadata, "docVersion", doc.docVersion());
        putIfPresent(metadata, "embeddingModel", embeddingModel);
        metadata.put("embeddingDims", embeddingDims);
        metadata.put("key", key);
        metadata.put("contentHash", contentHash);
        return new Chunk(key, id, text, metadata, contentHash);
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static String capitaliseFirst(String value) {
        return value.isEmpty() ? "None" : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (nonBlank(value)) {
            metadata.put(key, value);
        }
    }
}
