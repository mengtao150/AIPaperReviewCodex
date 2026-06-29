package com.aipaperreview.service.llm;

public record LlmProviderTestResult(
        boolean success,
        String content,
        long durationMs,
        String errorMessage
) {
}
