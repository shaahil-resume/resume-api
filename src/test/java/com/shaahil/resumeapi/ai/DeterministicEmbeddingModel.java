package com.shaahil.resumeapi.ai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DeterministicEmbeddingModel implements EmbeddingModel {

    private final AtomicInteger embeddedTexts = new AtomicInteger();

    @Override
    public float[] embed(Document document) {
        return call(new EmbeddingRequest(List.of(document.getText()), EmbeddingOptions.builder().build()))
                .getResult().getOutput();
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options,
                               BatchingStrategy batchingStrategy) {
        EmbeddingOptions effectiveOptions = options == null ? EmbeddingOptions.builder().build() : options;
        return call(new EmbeddingRequest(
                documents.stream().map(Document::getText).toList(), effectiveOptions))
                .getResults().stream()
                .map(Embedding::getOutput)
                .toList();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        // Count texts rather than calls because PgVectorStore may batch multiple documents in one request.
        embeddedTexts.addAndGet(request.getInstructions().size());
        List<Embedding> embeddings = new ArrayList<>();
        for (int index = 0; index < request.getInstructions().size(); index++) {
            embeddings.add(new Embedding(vectorFor(request.getInstructions().get(index)), index));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public int dimensions() {
        return 1536;
    }

    public int embeddedTexts() {
        return embeddedTexts.get();
    }

    public void reset() {
        embeddedTexts.set(0);
    }

    private static float[] vectorFor(String text) {
        float[] vector = new float[1536];
        String[] words = text.toLowerCase(Locale.ROOT).split("[^\\p{Alnum}]+");
        boolean hasWord = false;
        for (String word : words) {
            if (!word.isBlank()) {
                hasWord = true;
                int index = Math.floorMod(word.hashCode(), vector.length);
                vector[index] += 1.0f;
            }
        }
        if (!hasWord) {
            vector[Math.floorMod(text.hashCode(), vector.length)] = 1.0f;
        }
        return vector;
    }
}
