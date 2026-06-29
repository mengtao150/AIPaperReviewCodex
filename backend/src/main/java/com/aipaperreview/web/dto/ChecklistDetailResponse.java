package com.aipaperreview.web.dto;

import java.util.List;

public record ChecklistDetailResponse(
        Long id,
        String name,
        String reviewType,
        String version,
        List<Item> items
) {
    public record Item(
            Long id,
            String category,
            String requirement,
            boolean required,
            String evaluationGuidance,
            int orderIndex
    ) {
    }
}
