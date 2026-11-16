package com.shaahil.resumeapi.ai.ingest;

import java.util.List;

public record KnowledgeDocument(
        String section,
        String company,
        String role,
        String startDate,
        String endDate,
        List<String> skills,
        String docVersion,
        String body
) {
}
