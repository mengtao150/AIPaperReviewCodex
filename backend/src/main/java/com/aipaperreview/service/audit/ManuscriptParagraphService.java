package com.aipaperreview.service.audit;

import com.aipaperreview.domain.AuditJob;
import com.aipaperreview.repository.AuditJobRepository;
import com.aipaperreview.service.document.DocumentExtractor;
import com.aipaperreview.service.document.ExtractedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ManuscriptParagraphService {
    private final List<DocumentExtractor> extractors;
    private final AuditJobRepository auditJobRepository;
    private final ObjectMapper objectMapper;

    public ManuscriptParagraphService(
            List<DocumentExtractor> extractors,
            AuditJobRepository auditJobRepository,
            ObjectMapper objectMapper
    ) {
        this.extractors = extractors;
        this.auditJobRepository = auditJobRepository;
        this.objectMapper = objectMapper;
    }

    public AuditJob ensureParagraphs(AuditJob job) {
        if (hasSavedParagraphs(job.getManuscriptParagraphsJson())) {
            return job;
        }
        if (job.getManuscriptFilePath() == null || job.getManuscriptFilePath().isBlank()) {
            return job;
        }
        Path manuscriptPath = Path.of(job.getManuscriptFilePath());
        if (!Files.exists(manuscriptPath)) {
            return job;
        }
        DocumentExtractor extractor = findExtractor(job.getOriginalFilename());
        if (extractor == null) {
            return job;
        }
        try (InputStream inputStream = Files.newInputStream(manuscriptPath)) {
            ExtractedDocument document = extractor.extract(job.getOriginalFilename(), inputStream);
            job.setManuscriptParagraphsJson(toJson(document.paragraphs()));
            return auditJobRepository.save(job);
        } catch (Exception ignored) {
            return job;
        }
    }

    public String toJson(List<String> paragraphs) {
        List<ManuscriptParagraph> responses = new ArrayList<>();
        if (paragraphs != null) {
            for (String text : paragraphs) {
                if (text == null || text.isBlank()) {
                    continue;
                }
                int orderIndex = responses.size() + 1;
                responses.add(new ManuscriptParagraph("p" + orderIndex, orderIndex, text.strip()));
            }
        }
        try {
            return objectMapper.writeValueAsString(responses);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private boolean hasSavedParagraphs(String value) {
        return value != null && !value.isBlank() && !"[]".equals(value.strip());
    }

    private DocumentExtractor findExtractor(String filename) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(filename))
                .findFirst()
                .orElse(null);
    }

    private record ManuscriptParagraph(String paragraphId, int orderIndex, String text) {
    }
}
