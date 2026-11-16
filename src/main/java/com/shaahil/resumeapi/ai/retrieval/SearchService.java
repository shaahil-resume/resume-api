package com.shaahil.resumeapi.ai.retrieval;

import com.shaahil.resumeapi.ai.AiProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final VectorStore vectorStore;
    private final AiProperties properties;

    public SearchService(VectorStore vectorStore, AiProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    public SearchOutcome search(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank");
        }

        AiProperties.Retrieval retrieval = properties.retrieval();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(retrieval.topK())
                .similarityThreshold(0.0)
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        double bestScore = results.stream()
                .mapToDouble(this::score)
                .max()
                .orElse(0.0);
        boolean aboveFloor = !results.isEmpty() && bestScore >= retrieval.floor();
        List<RetrievedChunk> chunks = results.stream()
                .map(this::toRetrievedChunk)
                .filter(chunk -> chunk.score() >= retrieval.threshold())
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(retrieval.keepTopN())
                .toList();
        return new SearchOutcome(aboveFloor, bestScore, chunks);
    }

    private RetrievedChunk toRetrievedChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Object key = metadata.get("key");
        return new RetrievedChunk(document.getId(), key == null ? null : key.toString(), document.getText(),
                score(document), metadata);
    }

    private double score(Document document) {
        return document.getScore() == null ? 0.0 : document.getScore();
    }
}
