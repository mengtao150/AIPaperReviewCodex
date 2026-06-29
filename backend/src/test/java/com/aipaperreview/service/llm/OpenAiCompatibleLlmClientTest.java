package com.aipaperreview.service.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.LlmProviderConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLlmClientTest {
    @Test
    void buildsOpenAiCompatibleChatPayload() {
        LlmProviderConfig config = new LlmProviderConfig();
        config.setModelName("gpt-test");
        config.setTemperature(0.2);
        config.setMaxTokens(2048);

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient();

        Map<String, Object> payload = client.buildPayload(
                config,
                new LlmRequest("system rules", "user manuscript")
        );

        assertThat(payload).containsEntry("model", "gpt-test");
        assertThat(payload).containsEntry("temperature", 0.2);
        assertThat(payload).containsEntry("max_tokens", 2048);
        List<?> messages = (List<?>) payload.get("messages");
        assertThat(messages).hasSize(2);
        Map<?, ?> systemMessage = (Map<?, ?>) messages.get(0);
        Map<?, ?> userMessage = (Map<?, ?>) messages.get(1);
        assertThat(systemMessage.get("role")).isEqualTo("system");
        assertThat(userMessage.get("content")).isEqualTo("user manuscript");
    }
}
