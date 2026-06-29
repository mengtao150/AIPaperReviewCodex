package com.aipaperreview.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxDocumentExtractorTest {
    @Test
    void extractsTitleAndParagraphsFromDocx() throws Exception {
        XWPFDocument document = new XWPFDocument();
        document.createParagraph().createRun().setText("Narrative review of lung cancer surgery");
        document.createParagraph().createRun().setText("Abstract");
        document.createParagraph().createRun().setText("This review summarizes current evidence.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        document.write(output);

        DocxDocumentExtractor extractor = new DocxDocumentExtractor();
        ExtractedDocument extracted = extractor.extract(
                "sample.docx",
                new ByteArrayInputStream(output.toByteArray())
        );

        assertThat(extracted.title()).isEqualTo("Narrative review of lung cancer surgery");
        assertThat(extracted.paragraphs()).containsExactly(
                "Narrative review of lung cancer surgery",
                "Abstract",
                "This review summarizes current evidence."
        );
        assertThat(extracted.fullText()).contains("current evidence");
    }
}
