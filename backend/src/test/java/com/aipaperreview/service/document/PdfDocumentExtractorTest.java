package com.aipaperreview.service.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

class PdfDocumentExtractorTest {
    @Test
    void extractsTextFromTextBasedPdf() throws IOException {
        byte[] pdf = createPdf("Narrative Review Manuscript", "Background and knowledge gap");

        ExtractedDocument document = new PdfDocumentExtractor()
                .extract("manuscript.pdf", new ByteArrayInputStream(pdf));

        assertThat(document.filename()).isEqualTo("manuscript.pdf");
        assertThat(document.title()).isEqualTo("Narrative Review Manuscript");
        assertThat(document.paragraphs()).contains("Narrative Review Manuscript", "Background and knowledge gap");
        assertThat(document.fullText()).contains("Narrative Review Manuscript", "Background and knowledge gap");
    }

    @Test
    void supportsPdfFilenameCaseInsensitively() {
        PdfDocumentExtractor extractor = new PdfDocumentExtractor();

        assertThat(extractor.supports("MANUSCRIPT.PDF")).isTrue();
        assertThat(extractor.supports("manuscript.docx")).isFalse();
    }

    private byte[] createPdf(String firstLine, String secondLine) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(72, 720);
                content.showText(firstLine);
                content.newLineAtOffset(0, -18);
                content.showText(secondLine);
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
