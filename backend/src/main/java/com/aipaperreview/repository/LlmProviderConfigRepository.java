package com.aipaperreview.repository;

import com.aipaperreview.domain.LlmProviderConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmProviderConfigRepository extends JpaRepository<LlmProviderConfig, Long> {
    List<LlmProviderConfig> findAllByOrderByCreatedAtDesc();

    Optional<LlmProviderConfig> findFirstByActiveTrueOrderByCreatedAtDesc();
}
