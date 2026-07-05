package com.aipaperreview.web.dto;

import com.aipaperreview.domain.AuditJob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;

public record AuditJobResponse(
        Long id,
        String originalFilename,
        String status,
        String reviewType,
        String classificationResult,
        String structureAuditResult,
        String checklistAuditResult,
        String ragflowEnhancedAuditResult,
        String finalReport,
        String errorMessage,
        int progressPercent,
        Instant createdAt,
        List<ManuscriptParagraphResponse> manuscriptParagraphs
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<ManuscriptParagraphResponse>> PARAGRAPH_LIST_TYPE = new TypeReference<>() {
    };

    public static AuditJobResponse from(AuditJob job) {
        return new AuditJobResponse(
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus().name(),
                job.getDetectedReviewType() == null ? null : job.getDetectedReviewType().getName(),
                job.getClassificationResult(),
                job.getStructureAuditResult(),
                job.getChecklistAuditResult(),
                job.getRagflowEnhancedAuditResult(),
                job.getFinalReport(),
                job.getErrorMessage(),
                progressPercent(job.getStatus()),
                job.getCreatedAt(),
                parseParagraphs(job.getManuscriptParagraphsJson())
        );
    }

    private static int progressPercent(AuditJob.Status status) {
        return switch (status) {
            case UPLOADED -> 10;
            case EXTRACTED -> 25;
            case CLASSIFIED -> 45;
            case STRUCTURE_AUDITED -> 65;
            case CHECKLIST_AUDITED -> 85;
            case COMPLETED -> 100;
            case FAILED -> 100;
        };
    }

    private static List<ManuscriptParagraphResponse> parseParagraphs(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, PARAGRAPH_LIST_TYPE);
        } catch (Exception ignored) {
            return List.of();
        }
    }
}
