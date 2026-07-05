package com.aipaperreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_job")
public class AuditJob {
    public enum Status {
        UPLOADED,
        EXTRACTED,
        CLASSIFIED,
        STRUCTURE_AUDITED,
        CHECKLIST_AUDITED,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String manuscriptFilePath;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Status status = Status.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detected_review_type_id")
    private ReviewType detectedReviewType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String classificationResult;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String structureAuditResult;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String checklistAuditResult;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String ragflowEnhancedAuditResult;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String manuscriptParagraphsJson;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String finalReport;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String errorMessage;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getManuscriptFilePath() {
        return manuscriptFilePath;
    }

    public void setManuscriptFilePath(String manuscriptFilePath) {
        this.manuscriptFilePath = manuscriptFilePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ReviewType getDetectedReviewType() {
        return detectedReviewType;
    }

    public void setDetectedReviewType(ReviewType detectedReviewType) {
        this.detectedReviewType = detectedReviewType;
    }

    public String getClassificationResult() {
        return classificationResult;
    }

    public void setClassificationResult(String classificationResult) {
        this.classificationResult = classificationResult;
    }

    public String getStructureAuditResult() {
        return structureAuditResult;
    }

    public void setStructureAuditResult(String structureAuditResult) {
        this.structureAuditResult = structureAuditResult;
    }

    public String getChecklistAuditResult() {
        return checklistAuditResult;
    }

    public void setChecklistAuditResult(String checklistAuditResult) {
        this.checklistAuditResult = checklistAuditResult;
    }

    public String getRagflowEnhancedAuditResult() {
        return ragflowEnhancedAuditResult;
    }

    public void setRagflowEnhancedAuditResult(String ragflowEnhancedAuditResult) {
        this.ragflowEnhancedAuditResult = ragflowEnhancedAuditResult;
    }

    public String getManuscriptParagraphsJson() {
        return manuscriptParagraphsJson;
    }

    public void setManuscriptParagraphsJson(String manuscriptParagraphsJson) {
        this.manuscriptParagraphsJson = manuscriptParagraphsJson;
    }

    public String getFinalReport() {
        return finalReport;
    }

    public void setFinalReport(String finalReport) {
        this.finalReport = finalReport;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
