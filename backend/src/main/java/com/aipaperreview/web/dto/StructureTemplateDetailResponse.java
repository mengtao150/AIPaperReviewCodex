package com.aipaperreview.web.dto;

public record StructureTemplateDetailResponse(
        Long id,
        String name,
        String reviewType,
        String version,
        String parsedSections,
        String requiredItems,
        String optionalItems
) {
}
