package com.aipaperreview.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LlmProviderRequest(
        @NotBlank String name,
        String providerType,
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String modelName,
        Double temperature,
        Integer maxTokens,
        Integer timeoutSeconds,
        Boolean active
) {
}
