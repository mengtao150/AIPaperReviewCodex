package com.aipaperreview.service.audit;

import com.aipaperreview.domain.AuditJob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

@Component
public class WordReportBuilder {
    public byte[] build(AuditJob job) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String markdown = job.getFinalReport();
            if (markdown == null || markdown.isBlank()) {
                markdown = "# 综述稿件审查报告\n\n暂无报告内容。";
            }
            appendMarkdown(document, markdown);
            document.write(output);
            return output.toByteArray();
        }
    }

    private void appendMarkdown(XWPFDocument document, String markdown) {
        boolean codeBlock = false;
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.stripTrailing();
            if (line.strip().startsWith("```")) {
                codeBlock = !codeBlock;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            if (!codeBlock && line.startsWith("# ")) {
                addParagraph(document, line.substring(2).trim(), 18, true, ParagraphAlignment.CENTER, false);
            } else if (!codeBlock && line.startsWith("## ")) {
                addParagraph(document, line.substring(3).trim(), 14, true, ParagraphAlignment.LEFT, false);
            } else {
                addParagraph(document, line, 11, false, ParagraphAlignment.LEFT, codeBlock);
            }
        }
    }

    private void addParagraph(
            XWPFDocument document,
            String text,
            int fontSize,
            boolean bold,
            ParagraphAlignment alignment,
            boolean monospace
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setSpacingAfter(160);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily(monospace ? "Consolas" : "Microsoft YaHei");
    }
}
