package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkFactoryTest {

    @Test
    void slugNormalisesValues() {
        assertThat(ChunkFactory.slug(" Senior Engineer, Acme ")).isEqualTo("senior-engineer-acme");
        assertThat(ChunkFactory.slug("!!!")).isEqualTo("none");
        assertThat(ChunkFactory.slug(null)).isEqualTo("none");
        assertThat(ChunkFactory.slug("   ")).isEqualTo("none");
        assertThat(ChunkFactory.slug("already-lower-case")).isEqualTo("already-lower-case");
    }

    @Test
    void keyIsLowerCaseAndUsesNoneForMissingCompany() {
        KnowledgeDocument document = document("Experience", null, "Senior Engineer", "Shaahil Abraham");

        assertThat(ChunkFactory.key(document, 3)).isEqualTo("experience:none:senior-engineer:3");
        assertThat(ChunkFactory.key(document, 3)).doesNotContain(" ");
    }

    @Test
    void idIsDeterministicForAKey() {
        UUID first = ChunkFactory.id("experience:acme:engineer:0");

        assertThat(first).isEqualTo(ChunkFactory.id("experience:acme:engineer:0"));
        assertThat(first).isNotEqualTo(ChunkFactory.id("experience:acme:engineer:1"));
    }

    @Test
    void hashIsStableForTheSameDocument() {
        assertThat(ChunkFactory.hash(document("experience", "Acme", "Engineer", "Shaahil Abraham")))
                .isEqualTo(ChunkFactory.hash(document("experience", "Acme", "Engineer", "Shaahil Abraham")));
    }

    @Test
    void changedBodyChangesHash() {
        KnowledgeDocument original = document("experience", "Acme", "Engineer", "Shaahil Abraham");
        KnowledgeDocument changed = new KnowledgeDocument(original.section(), original.company(), original.role(),
                original.startDate(), original.endDate(), original.skills(), original.docVersion(), "A different body.");

        assertThat(ChunkFactory.hash(changed)).isNotEqualTo(ChunkFactory.hash(original));
    }

    @Test
    void changedCompanyChangesHash() {
        KnowledgeDocument original = document("experience", "Acme", "Engineer", "Shaahil Abraham");
        KnowledgeDocument changed = new KnowledgeDocument(original.section(), "Other Corp", original.role(),
                original.startDate(), original.endDate(), original.skills(), original.docVersion(), original.body());

        assertThat(ChunkFactory.hash(changed)).isNotEqualTo(ChunkFactory.hash(original));
    }

    @Test
    void changedDocVersionAloneDoesNotChangeHash() {
        KnowledgeDocument original = document("experience", "Acme", "Engineer", "Shaahil Abraham");
        KnowledgeDocument changed = new KnowledgeDocument(original.section(), original.company(), original.role(),
                original.startDate(), original.endDate(), original.skills(), "2", original.body());

        assertThat(ChunkFactory.hash(changed)).isEqualTo(ChunkFactory.hash(original));
    }

    @Test
    void headerExcludesCandidateNameAndSupportsProjectStyleDocuments() {
        KnowledgeDocument experience = document("experience", "Acme", "Senior Engineer", "Shaahil Abraham");
        KnowledgeDocument project = new KnowledgeDocument("project", null, null, null, null, List.of(), "1",
                "A project body.");

        assertThat(ChunkFactory.header(experience)).isEqualTo("Senior Engineer at Acme 2021-01 to 2024:");
        assertThat(ChunkFactory.header(experience)).doesNotContain("Shaahil Abraham");
        assertThat(ChunkFactory.header(project)).isEqualTo("Project:");
    }

    @Test
    void buildIncludesRequiredMetadataAndOmitsBlankValues() {
        KnowledgeDocument document = new KnowledgeDocument(
                "experience", "", " ", "2021-01", "2024", List.of("Java"), " ", "Body");

        Chunk chunk = ChunkFactory.build(document, 2);

        assertThat(chunk.metadata())
                .containsEntry("key", chunk.key())
                .containsEntry("contentHash", chunk.contentHash())
                .containsEntry("section", "experience")
                .containsEntry("skills", List.of("Java"))
                .doesNotContainKeys("company", "role", "docVersion");
    }

    private static KnowledgeDocument document(String section, String company, String role, String candidateName) {
        return new KnowledgeDocument(section, company, role, "2021-01", "2024", List.of("Java"), "1",
                "Built systems with " + candidateName + ".");
    }
}
