package com.shaahil.resumeapi.ai.ingest;

import java.util.Map;
import java.util.UUID;

public record Chunk(String key, UUID id, String text, Map<String, Object> metadata, String contentHash) {
}
