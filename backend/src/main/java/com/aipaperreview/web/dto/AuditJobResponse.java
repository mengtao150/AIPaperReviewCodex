package com.aipaperreview.web.dto;

import com.aipaperreview.domain.AuditJob;

public record AuditJobResponse(
        Long id,
        String originalFilename,
        String status,
        String reviewType,
        String classificationResult,
        String structureAuditResult,
        String checklistAuditResult,
        String finalReport,
        String errorMessage,
        int progressPercent
) {
    public static AuditJobResponse from(AuditJob job) {
        return new AuditJobResponse(
                job.getId(),
                job.getOriginalFilename(),
                job.getStatus().name(),
                job.getDetectedReviewType() == null ? null : job.getDetectedReviewType().getName(),
                job.getClassificationResult(),
                job.getStructureAuditResult(),
                job.getChecklistAuditResult(),
                job.getFinalReport(),
                job.getErrorMessage(),
                progressPercent(job.getStatus())
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
}
