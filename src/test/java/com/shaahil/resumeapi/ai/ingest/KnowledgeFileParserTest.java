package com.shaahil.resumeapi.ai.ingest;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class KnowledgeFileParserTest {

    private final KnowledgeFileParser parser = new KnowledgeFileParser();

    @Test
    void parsesEveryFixtureWithExpectedFields() {
        assertThat(parser.loadAll("classpath*:knowledge-fixtures/*.md"))
                .containsExactlyInAnyOrder(
                        document("education", "Example Institute", "Sample Student", "2018-01", "2019-12",
                                List.of("Imaginary Research", "Test Writing"),
                                "Completed a fictional program at Example Institute for exercising section-aware fixture ingestion."),
                        document("experience", "Acme Corp", "Example Engineer", "2020-01", "2021-12",
                                List.of("Fake Framework", "Sample Testing"),
                                "Worked on a fictional Acme Corp service that automated imaginary reports for a test fixture."),
                        document("experience", "Acme Corp", "Placeholder Technical Lead", "2022-01", "2022-12",
                                List.of("Mock Architecture", "Fixture Planning"),
                                "Led a fictional Acme Corp team that maintained a deliberately fake platform for ingestion tests."),
                        document("project", "Acme Corp", "Example Project Builder", "2023-01", "2023-03",
                                List.of("Fake Search", "Vector Fixtures"),
                                "Built a fictional Acme Corp search demo using invented documents and fake similarity results."),
                        document("skills", "Acme Corp", "Example Specialist", "2020-01", "2023-03",
                                List.of("Pretend Java", "Imaginary PostgreSQL"),
                                "This fictional skill profile exists only to test retrieval across a third knowledge section.")
                );
    }

    @Test
    void missingFrontMatterThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> parser.parse("No front matter here."))
                .withMessageContaining("file content")
                .withMessageContaining("front matter");
    }

    @Test
    void loadAllReturnsAllFixtureFiles() {
        assertThat(parser.loadAll("classpath*:knowledge-fixtures/*.md")).hasSize(5);
    }

    private static KnowledgeDocument document(
            String section,
            String company,
            String role,
            String startDate,
            String endDate,
            List<String> skills,
            String body
    ) {
        return new KnowledgeDocument(section, company, role, startDate, endDate, skills, "1", body);
    }
}
