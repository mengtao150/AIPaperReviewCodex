package com.aipaperreview.service.ragflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RagflowRetrievalClient implements RagflowKnowledgeClient {
    private final RagflowProperties properties;
    private final ObjectMapper objectMapper;

    public RagflowRetrievalClient(RagflowProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled()
                && properties.getApiKey() != null
                && !properties.getApiKey().isBlank()
                && !properties.parsedDatasetIds().isEmpty();
    }

    @Override
    public List<RagflowRetrievedChunk> retrieve(String question) {
        if (!isEnabled()) {
            return List.of();
        }

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        RestClient client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> response = client.post()
                .uri("/api/v1/retrieval")
                .body(buildPayload(question))
                .retrieve()
                .body(Map.class);

        return extractChunks(response);
    }

    Map<String, Object> buildPayload(String question) {
        return Map.of(
                "question", question,
                "dataset_ids", properties.parsedDatasetIds(),
                "page", 1,
                "page_size", properties.getPageSize(),
                "top_k", properties.getTopK(),
                "similarity_threshold", properties.getSimilarityThreshold(),
                "vector_similarity_weight", properties.getVectorSimilarityWeight()
        );
    }

    List<RagflowRetrievedChunk> extractChunks(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object data = response.get("data");
        Object chunks = null;
        if (data instanceof Map<?, ?> dataMap) {
            chunks = dataMap.get("chunks");
        } else if (data instanceof List<?>) {
            chunks = data;
        }
        if (chunks == null) {
            chunks = response.get("chunks");
        }
        if (!(chunks instanceof List<?> chunkList)) {
            return List.of();
        }

        List<RagflowRetrievedChunk> results = new ArrayList<>();
        for (Object item : chunkList) {
            Map<?, ?> chunk = objectMapper.convertValue(item, Map.class);
            String content = stringValue(chunk.get("content"));
            if (content.isBlank()) {
                continue;
            }
            results.add(new RagflowRetrievedChunk(
                    content,
                    firstNonBlank(
                            stringValue(chunk.get("document_name")),
                            stringValue(chunk.get("doc_name")),
                            stringValue(chunk.get("name"))
                    ),
                    doubleValue(chunk.get("similarity"))
            ));
        }
        return results;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null || baseUrl.isBlank() ? "http://localhost:9380" : baseUrl.strip();
        if (normalized.endsWith("/api/v1/retrieval")) {
            normalized = normalized.substring(0, normalized.length() - "/api/v1/retrieval".length());
        }
        if (normalized.endsWith("/api/v1")) {
            normalized = normalized.substring(0, normalized.length() - "/api/v1".length());
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString().strip();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
