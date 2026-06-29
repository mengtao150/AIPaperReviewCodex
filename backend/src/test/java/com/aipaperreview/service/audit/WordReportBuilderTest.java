package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.AuditJob;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class WordReportBuilderTest {
    @Test
    void buildsWordDocumentFromAuditJobMarkdownReport() throws IOException {
        AuditJob job = new AuditJob();
        job.setOriginalFilename("manuscript.pdf");
        job.setFinalReport("""
                # 综述稿件审查报告

                ## 稿件

                manuscript.pdf

                ## 修改建议

                建议补充检索策略。
                """);

        byte[] bytes = new WordReportBuilder().build(job);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText())
                    .reduce("", (left, right) -> left + "\n" + right);

            assertThat(text).contains("综述稿件审查报告");
            assertThat(text).contains("manuscript.pdf");
            assertThat(text).contains("建议补充检索策略");
        }
    }
}
