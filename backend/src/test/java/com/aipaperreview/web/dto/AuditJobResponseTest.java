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
}
