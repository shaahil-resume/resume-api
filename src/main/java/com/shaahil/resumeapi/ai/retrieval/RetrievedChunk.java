package com.shaahil.resumeapi.ai.retrieval;

import java.util.Map;

public record RetrievedChunk(String id, String key, String text, double score, Map<String, Object> metadata) {
}
