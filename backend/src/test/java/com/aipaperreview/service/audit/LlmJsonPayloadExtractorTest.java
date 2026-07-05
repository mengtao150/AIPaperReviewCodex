package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LlmJsonPayloadExtractorTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void doesNotSilentlyExtractFirstObjectFromMalformedArray() {
        String response = """
                ```json
                [
                  {"item":"Title Page","status":"撰写不完整"},
                  {"item":"Abstract","status":"已撰写"}
                ```
                """;

        Optional<JsonNode> extracted = LlmJsonPayloadExtractor.extract(objectMapper, response);

        assertThat(extracted).isEmpty();
    }

    @Test
    void completeExtractionDoesNotTakeFirstObjectFromMixedAuditResponse() {
        String response = """
                <think>For item 13 (主要内容): 内容科学性 and 逻辑性 need detailed review.</think>
                {"item":"题目","status":"已撰写"}
                {"item":"摘要","status":"已撰写"}
                """;

        Optional<JsonNode> extracted = LlmJsonPayloadExtractor.extractComplete(objectMapper, response);

        assertThat(extracted).isEmpty();
    }

    @Test
    void completeExtractionKeepsValidFencedArray() {
        String response = """
                <think>analysis</think>
                ```json
                [
                  {"item":"题目","status":"已撰写"},
                  {"item":"摘要","status":"撰写不完整"}
                ]
                ```
                """;

        Optional<JsonNode> extracted = LlmJsonPayloadExtractor.extractComplete(objectMapper, response);

        assertThat(extracted).isPresent();
        assertThat(extracted.orElseThrow().isArray()).isTrue();
        assertThat(extracted.orElseThrow()).hasSize(2);
    }
}
