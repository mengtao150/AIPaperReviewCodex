package com.aipaperreview.service.ragflow;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ragflow")
public class RagflowProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:9380";
    private String apiKey = "";
    private String datasetIds = "";
    private int topK = 8;
    private int pageSize = 8;
    private double similarityThreshold = 0.2;
    private double vectorSimilarityWeight = 0.3;
    private int timeoutSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDatasetIds() {
        return datasetIds;
    }

    public void setDatasetIds(String datasetIds) {
        this.datasetIds = datasetIds;
    }

    public List<String> parsedDatasetIds() {
        if (datasetIds == null || datasetIds.isBlank()) {
            return List.of();
        }
        return Arrays.stream(datasetIds.split(","))
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public double getVectorSimilarityWeight() {
        return vectorSimilarityWeight;
    }

    public void setVectorSimilarityWeight(double vectorSimilarityWeight) {
        this.vectorSimilarityWeight = vectorSimilarityWeight;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
