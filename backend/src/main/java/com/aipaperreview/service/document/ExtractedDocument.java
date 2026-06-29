package com.aipaperreview.service.document;

import java.util.List;

public record ExtractedDocument(
        String filename,
        String title,
        List<String> paragraphs,
        String fullText
) {
}
