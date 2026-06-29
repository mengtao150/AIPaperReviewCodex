package com.aipaperreview.web.dto;

import com.aipaperreview.domain.LlmProviderConfig;

public record LlmProviderResponse(
        Long id,
        String name,
        String providerType,
        String baseUrl,
        String modelName,
        double temperature,
        int maxTokens,
        int timeoutSeconds,
        boolean active
) {
    public static LlmProviderResponse from(LlmProviderConfig config) {
        return new LlmProviderResponse(
                config.getId(),
                config.getName(),
                config.getProviderType(),
                config.getBaseUrl(),
                config.getModelName(),
                config.getTemperature(),
                config.getMaxTokens(),
                config.getTimeoutSeconds(),
                config.isActive()
        );
    }
}
