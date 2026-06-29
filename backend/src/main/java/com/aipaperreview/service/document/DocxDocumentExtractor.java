package com.aipaperreview.service.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

@Component
public class DocxDocumentExtractor implements DocumentExtractor {
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".docx");
    }

    @Override
    public ExtractedDocument extract(String filename, InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            List<String> paragraphs = new ArrayList<>();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    paragraphs.add(text.trim());
                }
            }
            String title = paragraphs.isEmpty() ? filename : paragraphs.get(0);
            return new ExtractedDocument(filename, title, paragraphs, String.join("\n", paragraphs));
        }
    }
}
