package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipaperreview.domain.AuditJob;
import com.aipaperreview.repository.AuditJobRepository;
import com.aipaperreview.service.document.DocumentExtractor;
import com.aipaperreview.service.document.ExtractedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManuscriptParagraphServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesFullManuscriptParagraphsWithStableIds() throws Exception {
        AuditJobRepository repository = mock(AuditJobRepository.class);
        ManuscriptParagraphService service = new ManuscriptParagraphService(List.of(), repository, objectMapper);

        String json = service.toJson(List.of("  Abstract text  ", "", "Main body text"));

        JsonNode root = objectMapper.readTree(json);
        assertThat(root).hasSize(2);
        assertThat(root.get(0).get("paragraphId").asText()).isEqualTo("p1");
        assertThat(root.get(0).get("orderIndex").asInt()).isEqualTo(1);
        assertThat(root.get(0).get("text").asText()).isEqualTo("Abstract text");
        assertThat(root.get(1).get("paragraphId").asText()).isEqualTo("p2");
        assertThat(root.get(1).get("text").asText()).isEqualTo("Main body text");
    }

    @Test
    void hydratesExistingJobParagraphsFromSavedManuscriptFile() throws Exception {
        Path manuscript = Files.createTempFile("manuscript", ".docx");
        Files.writeString(manuscript, "raw manuscript bytes");
        DocumentExtractor extractor = new DocumentExtractor() {
            @Override
            public boolean supports(String filename) {
                return filename.endsWith(".docx");
            }

            @Override
            public ExtractedDocument extract(String filename, InputStream inputStream) throws IOException {
                return new ExtractedDocument(
                        filename,
                        "Hydrated title",
                        List.of("First section text", "Second section text"),
                        "First section text\nSecond section text"
                );
            }
        };
        AuditJobRepository repository = mock(AuditJobRepository.class);
        when(repository.save(any(AuditJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ManuscriptParagraphService service = new ManuscriptParagraphService(List.of(extractor), repository, objectMapper);
        AuditJob job = new AuditJob();
        job.setOriginalFilename("history.docx");
        job.setManuscriptFilePath(manuscript.toString());

        AuditJob hydrated = service.ensureParagraphs(job);

        assertThat(hydrated.getManuscriptParagraphsJson()).contains("First section text");
        assertThat(hydrated.getManuscriptParagraphsJson()).contains("\"paragraphId\":\"p2\"");
        verify(repository).save(job);
    }
}
