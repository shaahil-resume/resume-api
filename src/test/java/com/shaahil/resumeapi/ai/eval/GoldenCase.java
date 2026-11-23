package com.shaahil.resumeapi.ai.eval;

import java.util.List;
import java.util.Map;

public record GoldenCase(
        String id,
        String category,
        String split,
        String question,
        String expectedRoute,
        List<String> expectedKeyContains,
        List<String> mustMatch,
        List<String> mustNotMatch,
        String note) {

    public static GoldenCase from(Map<String, Object> values) {
        return new GoldenCase(
                string(values, "id"),
                string(values, "category"),
                string(values, "split"),
                string(values, "question"),
                values.containsKey("expectedRoute") ? string(values, "expectedRoute") : "ANSWER",
                strings(values.get("expectedKeyContains")),
                strings(values.get("mustMatch")),
                strings(values.get("mustNotMatch")),
                values.containsKey("note") ? string(values, "note") : null);
    }

    private static String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(Object::toString).toList();
    }
}
