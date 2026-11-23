package com.shaahil.resumeapi.ai.eval;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class GoldenSetLoader {

    private GoldenSetLoader() {
    }

    public static List<GoldenCase> load() {
        InputStream stream = GoldenSetLoader.class.getClassLoader().getResourceAsStream("eval/golden.yaml");
        if (stream == null) {
            throw new IllegalStateException("Missing eval/golden.yaml");
        }
        try (stream) {
            Object parsed = new Yaml().load(stream);
            if (!(parsed instanceof Map<?, ?> root) || !(root.get("cases") instanceof List<?> cases)) {
                throw new IllegalArgumentException("golden.yaml must contain a cases list");
            }
            return cases.stream()
                    .map(caseValue -> GoldenCase.from(castMap(caseValue)))
                    .toList();
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("Could not parse eval/golden.yaml", exception);
        }
    }

    private static Map<String, Object> castMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Each golden case must be a map");
        }
        return map.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                entry -> entry.getKey().toString(), Map.Entry::getValue));
    }
}
