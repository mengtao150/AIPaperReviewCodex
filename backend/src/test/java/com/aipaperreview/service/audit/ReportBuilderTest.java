package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportBuilderTest {
    @Test
    void buildsMarkdownReportWithMissingTemplateMessage() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                "{}",
                Optional.empty(),
                "{}"
        );

        assertThat(markdown).contains("# 综述稿件审查报告");
        assertThat(markdown).contains("manuscript.docx");
        assertThat(markdown).contains("未匹配到结构模板");
        assertThat(markdown).contains("未匹配到审核清单");
    }
}
