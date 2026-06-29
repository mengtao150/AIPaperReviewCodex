package com.aipaperreview.web;

import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.service.llm.LlmProviderService;
import com.aipaperreview.service.llm.LlmProviderTestResult;
import com.aipaperreview.web.dto.LlmProviderRequest;
import com.aipaperreview.web.dto.LlmProviderResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/llm-providers")
public class LlmProviderController {
    private final LlmProviderService service;

    public LlmProviderController(LlmProviderService service) {
        this.service = service;
    }

    @GetMapping
    public List<LlmProviderResponse> list() {
        return service.list().stream().map(LlmProviderResponse::from).toList();
    }

    @PostMapping
    public LlmProviderResponse save(@Valid @RequestBody LlmProviderRequest request) {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setName(request.name());
        config.setProviderType(request.providerType() == null || request.providerType().isBlank()
                ? "OPENAI_COMPATIBLE"
                : request.providerType());
        config.setBaseUrl(request.baseUrl());
        config.setApiKey(request.apiKey());
        config.setModelName(request.modelName());
        config.setTemperature(request.temperature() == null ? 0.1 : request.temperature());
        config.setMaxTokens(request.maxTokens() == null ? 4096 : request.maxTokens());
        config.setTimeoutSeconds(request.timeoutSeconds() == null ? 120 : request.timeoutSeconds());
        config.setActive(Boolean.TRUE.equals(request.active()));
        return LlmProviderResponse.from(service.save(config));
    }

    @PostMapping("/{id}/activate")
    public LlmProviderResponse activate(@PathVariable Long id) {
        return LlmProviderResponse.from(service.activate(id));
    }

    @PostMapping("/{id}/test")
    public LlmProviderTestResult test(@PathVariable Long id) {
        return service.testProvider(id);
    }
}
