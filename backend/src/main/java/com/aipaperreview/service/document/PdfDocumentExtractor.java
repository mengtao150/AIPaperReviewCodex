package com.aipaperreview.service.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    @Override
    public ExtractedDocument extract(String filename, InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String text;
        try (PDDocument document = Loader.loadPDF(bytes)) {
            text = new PDFTextStripper().getText(document);
        }
        List<String> paragraphs = Arrays.stream(text.split("\\R+"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        if (paragraphs.isEmpty()) {
            throw new IllegalArgumentException("PDF 未提取到可审查文本，请上传可复制文本的 PDF，或先 OCR 后再上传");
        }
        String title = paragraphs.get(0);
        return new ExtractedDocument(filename, title, paragraphs, String.join("\n", paragraphs));
    }
}
