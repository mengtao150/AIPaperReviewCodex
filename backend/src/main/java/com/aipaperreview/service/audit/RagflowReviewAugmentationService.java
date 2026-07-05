package com.aipaperreview.service.audit;

import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.service.document.ExtractedDocument;
import com.aipaperreview.service.llm.LlmClient;
import com.aipaperreview.service.llm.LlmRequest;
import com.aipaperreview.service.ragflow.RagflowKnowledgeClient;
import com.aipaperreview.service.ragflow.RagflowRetrievedChunk;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RagflowReviewAugmentationService {
    private static final int MAX_MANUSCRIPT_CHARS = 18000;
    private static final int MAX_CHUNK_CHARS = 1200;

    private final RagflowKnowledgeClient ragflowKnowledgeClient;
    private final LlmClient llmClient;

    public RagflowReviewAugmentationService(RagflowKnowledgeClient ragflowKnowledgeClient, LlmClient llmClient) {
        this.ragflowKnowledgeClient = ragflowKnowledgeClient;
        this.llmClient = llmClient;
    }

    public Optional<String> audit(LlmProviderConfig provider, String reviewTypeName, ExtractedDocument document) {
        if (!ragflowKnowledgeClient.isEnabled()) {
            return Optional.empty();
        }
        try {
            List<RagflowRetrievedChunk> chunks = ragflowKnowledgeClient.retrieve(buildRetrievalQuery(reviewTypeName, document));
            if (chunks.isEmpty()) {
                return Optional.of("""
                        {"status":"no_ragflow_matches","message":"RAGFlow 未检索到可用的历史人工审稿意见"}
                        """);
            }
            String prompt = buildEnhancedAuditPrompt(reviewTypeName, document, chunks);
            String response = llmClient.complete(
                    provider,
                    new LlmRequest("你是医学期刊资深审稿人，擅长依据历史人工审稿意见发现当前稿件的实质性问题。", prompt)
            ).content();
            return Optional.of(response);
        } catch (Exception ex) {
            return Optional.of("""
                    {"status":"ragflow_error","message":"RAGFlow 人工经验增强审查失败：%s"}
                    """.formatted(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage().replace("\"", "'")));
        }
    }

    static String buildRetrievalQuery(String reviewTypeName, ExtractedDocument document) {
        return """
                文章类型：%s
                稿件标题：%s
                请检索相似历史人工审稿意见，重点关注：文章结构、审核清单、主要内容、科学性、逻辑性、证据支撑、综述分析深度、图表与正文呼应、创新性、临床决策框架。

                稿件摘录：
                %s
                """.formatted(
                blankToDefault(reviewTypeName, "未知综述类型"),
                document.title(),
                clip(document.fullText(), 4000)
        );
    }

    static String buildEnhancedAuditPrompt(
            String reviewTypeName,
            ExtractedDocument document,
            List<RagflowRetrievedChunk> chunks
    ) {
        return """
                请结合“当前稿件全文”和“历史人工审稿意见”，对当前稿件进行人工经验增强审查。

                重要要求：
                1. 历史人工审稿意见只能作为审稿角度参考，不能照抄，不能把历史稿件的问题直接套用到当前稿件。
                2. 必须围绕当前稿件原文给出具体问题、依据和修改建议。
                3. 优先发现实质性问题，例如内容科学性、逻辑性、证据支撑、综述分析深度、图表与正文呼应、创新性、临床决策框架、与既往综述差异、关键数据一致性。
                4. 按 Major问题、Minor问题、格式与表述问题 分层输出；没有证据时不要强行提出问题。
                5. 只输出合法 JSON 数组，不要输出 Markdown，不要输出思考过程。
                6. 每个数组元素必须包含 item, status, evidence, reason, suggestion, suggested_location。
                7. status 只能使用：Major问题、Minor问题、格式与表述问题、无法判断。

                文章类型：
                %s

                历史人工审稿意见：
                %s

                当前稿件全文：
                %s
                """.formatted(
                blankToDefault(reviewTypeName, "未知综述类型"),
                formatChunks(chunks),
                clip(document.fullText(), MAX_MANUSCRIPT_CHARS)
        );
    }

    private static String formatChunks(List<RagflowRetrievedChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(chunks.size(), 8);
        for (int index = 0; index < limit; index++) {
            RagflowRetrievedChunk chunk = chunks.get(index);
            builder.append(index + 1)
                    .append(". 来源：")
                    .append(blankToDefault(chunk.documentName(), "未命名历史审稿意见"))
                    .append("；相似度：")
                    .append(String.format(java.util.Locale.ROOT, "%.2f", chunk.similarity()))
                    .append("\n")
                    .append(clip(chunk.content(), MAX_CHUNK_CHARS))
                    .append("\n\n");
        }
        return builder.toString().stripTrailing();
    }

    private static String clip(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxChars) + "\n……（已截断）";
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}
