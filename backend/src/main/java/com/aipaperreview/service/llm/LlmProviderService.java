package com.aipaperreview.service.llm;

import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.repository.LlmProviderConfigRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LlmProviderService {
    private final LlmProviderConfigRepository repository;
    private final LlmClient llmClient;

    public LlmProviderService(LlmProviderConfigRepository repository, LlmClient llmClient) {
        this.repository = repository;
        this.llmClient = llmClient;
    }

    public List<LlmProviderConfig> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public LlmProviderConfig activeProvider() {
        return repository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new IllegalStateException("请先配置并启用一个大模型供应商"));
    }

    @Transactional
    public LlmProviderConfig save(LlmProviderConfig config) {
        if (config.isActive()) {
            List<LlmProviderConfig> configs = repository.findAll();
            for (LlmProviderConfig existing : configs) {
                existing.setActive(false);
            }
            repository.saveAll(configs);
        }
        return repository.save(config);
    }

    @Transactional
    public LlmProviderConfig activate(Long id) {
        List<LlmProviderConfig> configs = repository.findAll();
        for (LlmProviderConfig config : configs) {
            config.setActive(config.getId().equals(id));
        }
        repository.saveAll(configs);
        return repository.findById(id).orElseThrow();
    }

    public LlmProviderTestResult testProvider(Long id) {
        LlmProviderConfig config = repository.findById(id).orElseThrow();
        long startedAt = System.nanoTime();
        try {
            LlmResponse response = llmClient.complete(config, new LlmRequest(
                    "你是一个接口连通性测试助手。请严格按用户要求返回。",
                    "请只返回 OK，用于测试模型配置是否可用。"
            ));
            return new LlmProviderTestResult(
                    true,
                    response.content(),
                    elapsedMs(startedAt),
                    null
            );
        } catch (Exception ex) {
            return new LlmProviderTestResult(
                    false,
                    "",
                    elapsedMs(startedAt),
                    ex.getMessage()
            );
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }
}
