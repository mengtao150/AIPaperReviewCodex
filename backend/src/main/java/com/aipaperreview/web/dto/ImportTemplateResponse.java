package com.aipaperreview.web.dto;

public record ImportTemplateResponse(
        Long id,
        String name,
        String reviewType,
        String version,
        String parsedContent
) {
}
