package com.aipaperreview.service.ragflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RagflowRetrievalClientTest {
    @Test
    void buildsRetrievalPayloadFromConfiguredDatasetIds() {
        RagflowProperties properties = new RagflowProperties();
        properties.setDatasetIds("dataset-a,dataset-b");
        properties.setTopK(12);
        properties.setPageSize(5);
        properties.setSimilarityThreshold(0.35);
        properties.setVectorSimilarityWeight(0.7);
        RagflowRetrievalClient client = new RagflowRetrievalClient(properties, new ObjectMapper());

        Map<String, Object> payload = client.buildPayload("Narrative Review 人工审稿经验");

        assertThat(payload).containsEntry("question", "Narrative Review 人工审稿经验");
        assertThat(payload).containsEntry("page", 1);
        assertThat(payload).containsEntry("page_size", 5);
        assertThat(payload).containsEntry("top_k", 12);
        assertThat(payload).containsEntry("similarity_threshold", 0.35);
        assertThat(payload).containsEntry("vector_similarity_weight", 0.7);
        assertThat(payload.get("dataset_ids")).isEqualTo(List.of("dataset-a", "dataset-b"));
    }

    @Test
    void extractsReadableChunksFromRagflowResponse() {
        RagflowProperties properties = new RagflowProperties();
        RagflowRetrievalClient client = new RagflowRetrievalClient(properties, new ObjectMapper());
        Map<String, Object> response = Map.of(
                "code", 0,
                "data", Map.of(
                        "chunks", List.of(
                                Map.of(
                                        "content", "应补充研究设计、样本量和主要结局的总结表。",
                                        "document_name", "人工意见-001.docx",
                                        "similarity", 0.82
                                ),
                                Map.of(
                                        "content", "正文需要比较与既往综述的创新点。",
                                        "document_name", "人工意见-002.docx",
                                        "similarity", 0.76
                                )
                        )
                )
        );

        List<RagflowRetrievedChunk> chunks = client.extractChunks(response);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).content()).contains("研究设计");
        assertThat(chunks.get(0).documentName()).isEqualTo("人工意见-001.docx");
        assertThat(chunks.get(0).similarity()).isEqualTo(0.82);
    }
}
