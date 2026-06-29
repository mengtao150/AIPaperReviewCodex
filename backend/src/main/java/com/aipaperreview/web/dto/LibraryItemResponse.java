package com.aipaperreview.web.dto;

public record LibraryItemResponse(
        Long id,
        String name,
        String reviewType,
        String version,
        boolean active
) {
}
