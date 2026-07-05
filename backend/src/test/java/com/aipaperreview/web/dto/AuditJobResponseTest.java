package com.aipaperreview.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.AuditJob;
import org.junit.jupiter.api.Test;

class AuditJobResponseTest {
    @Test
    void mapsAuditStatusToProgressPercent() {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setManuscriptFilePath("tmp/manuscript.pdf");
        job.setStatus(AuditJob.Status.CLASSIFIED);

        AuditJobResponse response = AuditJobResponse.from(job);

        assertThat(response.progressPercent()).isEqualTo(45);
    }

    @Test
    void completedAuditReportsFullProgress() {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setManuscriptFilePath("tmp/manuscript.pdf");
        job.setStatus(AuditJob.Status.COMPLETED);

        AuditJobResponse response = AuditJobResponse.from(job);

        assertThat(response.progressPercent()).isEqualTo(100);
    }

    @Test
    void includesCreatedAtForHistoryList() {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("history.docx");
        job.setManuscriptFilePath("tmp/history.docx");
        job.setStatus(AuditJob.Status.COMPLETED);

        AuditJobResponse response = AuditJobResponse.from(job);

        assertThat(response.createdAt()).isEqualTo(job.getCreatedAt());
    }

    @Test
    void includesRagflowEnhancedAuditResult() {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("history.docx");
        job.setManuscriptFilePath("tmp/history.docx");
        job.setStatus(AuditJob.Status.COMPLETED);
        job.setRagflowEnhancedAuditResult("[{\"item\":\"Major问题\"}]");

        AuditJobResponse response = AuditJobResponse.from(job);

        assertThat(response.ragflowEnhancedAuditResult()).contains("Major问题");
    }

    @Test
    void includesManuscriptParagraphsForAnnotationView() {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("annotated.docx");
        job.setManuscriptFilePath("tmp/annotated.docx");
        job.setStatus(AuditJob.Status.COMPLETED);
        job.setManuscriptParagraphsJson("""
                [
                  {"paragraphId":"p1","orderIndex":1,"text":"Abstract Methods did not mention language limits."},
                  {"paragraphId":"p2","orderIndex":2,"text":"Main body only lists studies without critical appraisal."}
                ]
                """);

        AuditJobResponse response = AuditJobResponse.from(job);

        assertThat(response.manuscriptParagraphs()).hasSize(2);
        assertThat(response.manuscriptParagraphs().get(0).paragraphId()).isEqualTo("p1");
        assertThat(response.manuscriptParagraphs().get(1).text()).contains("critical appraisal");
    }
}
