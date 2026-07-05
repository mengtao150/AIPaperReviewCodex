package com.aipaperreview.web.dto;

public record ManuscriptParagraphResponse(
        String paragraphId,
        int orderIndex,
        String text
) {
}
