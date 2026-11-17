package com.shaahil.resumeapi.ai.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class IngestionService {

    public static final String DEFAULT_LOCATION = "classpath*:knowledge/deep-dive/*.md";

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final KnowledgeFileParser parser;
    private final String embeddingModel;
    private final int embeddingDims;
    private final PathMatchingResourcePatternResolver resourceResolver =
            new PathMatchingResourcePatternResolver();

    public IngestionService(
            VectorStore vectorStore,
            JdbcTemplate jdbcTemplate,
            KnowledgeFileParser parser,
            @Value("${spring.ai.openai.embedding.model:text-embedding-3-small}") String embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:1536}") int embeddingDims
    ) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.parser = parser;
        this.embeddingModel = embeddingModel;
        this.embeddingDims = embeddingDims;
    }

    public IngestionResult ingest() {
        return ingest(DEFAULT_LOCATION);
    }

    public IngestionResult ingest(String locationPattern) {
        List<NamedDocument> documents = loadDocuments(locationPattern);
        Map<String, String> filenamesByKey = new HashMap<>();
        List<Chunk> chunks = new ArrayList<>();
        for (NamedDocument namedDocument : documents) {
            Chunk chunk = ChunkFactory.build(namedDocument.document(), 0, embeddingModel, embeddingDims);
            String previousFilename = filenamesByKey.putIfAbsent(chunk.key(), namedDocument.filename());
            if (previousFilename != null) {
                throw new IllegalArgumentException("Duplicate chunk key '" + chunk.key() + "' in files '"
                        + previousFilename + "' and '" + namedDocument.filename() + "'");
            }
            chunks.add(chunk);
        }

        Map<UUID, String> storedHashes = readStoredState();
        Set<UUID> matchingIds = new HashSet<>();
        List<Chunk> toAdd = new ArrayList<>();
        List<String> toDelete = new ArrayList<>();
        int added = 0;
        int updated = 0;
        int unchanged = 0;

        for (Chunk chunk : chunks) {
            UUID chunkId = chunk.id();
            if (!storedHashes.containsKey(chunkId)) {
                toAdd.add(chunk);
                added++;
            } else if (Objects.equals(storedHashes.get(chunkId), chunk.contentHash())) {
                matchingIds.add(chunkId);
                unchanged++;
            } else {
                toDelete.add(chunkId.toString());
                toAdd.add(chunk);
                matchingIds.add(chunkId);
                updated++;
            }
        }

        for (UUID storedId : storedHashes.keySet()) {
            if (!matchingIds.contains(storedId)) {
                toDelete.add(storedId.toString());
            }
        }

        int deleted = toDelete.size() - updated;
        if (!toDelete.isEmpty()) {
            vectorStore.delete(toDelete);
        }
        if (!toAdd.isEmpty()) {
            vectorStore.add(toDocuments(toAdd));
        }

        log.info("Knowledge ingestion: added={}, updated={}, unchanged={}, deleted={}",
                added, updated, unchanged, deleted);
        return new IngestionResult(added, updated, unchanged, deleted);
    }

    private List<Document> toDocuments(List<Chunk> chunks) {
        return chunks.stream()
                .map(chunk -> new Document(chunk.id().toString(), chunk.text(), chunk.metadata()))
                .toList();
    }

    private Map<UUID, String> readStoredState() {
        Map<UUID, String> storedHashes = new HashMap<>();
        jdbcTemplate.query(
                "SELECT id, metadata->>'contentHash' AS content_hash FROM vector_store "
                        + "WHERE metadata->>'key' IS NOT NULL",
                (ResultSet resultSet, int rowNumber) -> {
                    storedHashes.put(resultSet.getObject("id", UUID.class), resultSet.getString("content_hash"));
                    return null;
                });
        return storedHashes;
    }

    private List<NamedDocument> loadDocuments(String locationPattern) {
        try {
            Resource[] resources = resourceResolver.getResources(locationPattern);
            List<NamedDocument> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || filename.equals("quick-facts.md") || filename.equals("README.md")) {
                    continue;
                }
                String content = resource.getContentAsString(StandardCharsets.UTF_8);
                documents.add(new NamedDocument(filename, parser.parse(content)));
            }
            return documents;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not load knowledge files from " + locationPattern, exception);
        }
    }

    private record NamedDocument(String filename, KnowledgeDocument document) {
    }
}
