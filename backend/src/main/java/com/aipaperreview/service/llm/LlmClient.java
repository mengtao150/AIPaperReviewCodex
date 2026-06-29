package com.aipaperreview.service.llm;

import com.aipaperreview.domain.LlmProviderConfig;

public interface LlmClient {
    LlmResponse complete(LlmProviderConfig config, LlmRequest request);
}
