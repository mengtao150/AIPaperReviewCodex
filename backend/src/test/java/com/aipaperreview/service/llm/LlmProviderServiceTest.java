package com.aipaperreview.service.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.repository.LlmProviderConfigRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LlmProviderServiceTest {
    @Test
    void testsProviderBySendingShortPromptAndReturningContent() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setId(7L);
        config.setName("Test Provider");
        config.setModelName("test-model");
        config.setTimeoutSeconds(30);
        LlmProviderConfigRepository repository = mock(LlmProviderConfigRepository.class);
        LlmClient llmClient = mock(LlmClient.class);
        when(repository.findById(7L)).thenReturn(Optional.of(config));
        when(llmClient.complete(eq(config), any(LlmRequest.class))).thenReturn(new LlmResponse("OK"));

        LlmProviderService service = new LlmProviderService(repository, llmClient);

        LlmProviderTestResult result = service.testProvider(7L);

        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("OK");
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void returnsFailureWhenProviderTestThrows() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setId(8L);
        LlmProviderConfigRepository repository = mock(LlmProviderConfigRepository.class);
        LlmClient llmClient = mock(LlmClient.class);
        when(repository.findById(8L)).thenReturn(Optional.of(config));
        when(llmClient.complete(eq(config), any(LlmRequest.class))).thenThrow(new IllegalStateException("bad api key"));

        LlmProviderService service = new LlmProviderService(repository, llmClient);

        LlmProviderTestResult result = service.testProvider(8L);

        assertThat(result.success()).isFalse();
        assertThat(result.content()).isBlank();
        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
        assertThat(result.errorMessage()).contains("bad api key");
    }
}
