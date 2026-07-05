package com.aipaperreview.service.ragflow;

public record RagflowRetrievedChunk(
        String content,
        String documentName,
        double similarity
) {
}
