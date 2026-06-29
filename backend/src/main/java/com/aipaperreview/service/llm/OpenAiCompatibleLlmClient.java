package com.aipaperreview.service.llm;

import com.aipaperreview.domain.LlmProviderConfig;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    @Override
    public LlmResponse complete(LlmProviderConfig config, LlmRequest request) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(config.getTimeoutSeconds()));
        RestClient client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(normalizeBaseUrl(config.getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> response = client.post()
                .uri("/chat/completions")
                .body(buildPayload(config, request))
                .retrieve()
                .body(Map.class);

        return new LlmResponse(extractContent(response));
    }

    Map<String, Object> buildPayload(LlmProviderConfig config, LlmRequest request) {
        return Map.of(
                "model", config.getModelName(),
                "temperature", config.getTemperature(),
                "max_tokens", config.getMaxTokens(),
                "messages", List.of(
                        Map.of("role", "system", "content", request.systemPrompt()),
                        Map.of("role", "user", "content", request.userPrompt())
                )
        );
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.strip();
        if (trimmed.endsWith("/chat/completions")) {
            return trimmed.substring(0, trimmed.length() - "/chat/completions".length());
        }
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            return "";
        }
        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return "";
        }
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object content = messageMap.get("content");
        return content == null ? "" : content.toString();
    }
}
