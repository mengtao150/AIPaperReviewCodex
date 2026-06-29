package com.aipaperreview.service.document;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class DefaultSourceTextExtractor implements SourceTextExtractor {
    private final DocxDocumentExtractor docxDocumentExtractor;

    public DefaultSourceTextExtractor(DocxDocumentExtractor docxDocumentExtractor) {
        this.docxDocumentExtractor = docxDocumentExtractor;
    }

    @Override
    public String extract(String filename, InputStream inputStream) throws IOException {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".docx")) {
            return docxDocumentExtractor.extract(filename, inputStream).fullText();
        }
        if (lower.endsWith(".pdf")) {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        throw new IllegalArgumentException("仅支持 .docx 或 .pdf 文件");
    }
}
