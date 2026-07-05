package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.service.document.ExtractedDocument;
import com.aipaperreview.service.ragflow.RagflowRetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagflowReviewAugmentationServiceTest {
    @Test
    void buildsRetrievalQueryFromReviewTypeTitleAndManuscriptText() {
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.pdf",
                "LCBDE versus ERCP narrative review",
                List.of("Abstract", "Main body"),
                "Abstract\nThis review compares LCBDE and ERCP.\nMain body\nEvidence and limitations."
        );

        String query = RagflowReviewAugmentationService.buildRetrievalQuery("Narrative Review", document);

        assertThat(query).contains("Narrative Review");
        assertThat(query).contains("LCBDE versus ERCP narrative review");
        assertThat(query).contains("人工审稿意见");
        assertThat(query).contains("科学性");
        assertThat(query).contains("逻辑性");
    }

    @Test
    void enhancedAuditPromptUsesHumanReviewExperienceWithoutCopyingIt() {
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.pdf",
                "LCBDE versus ERCP narrative review",
                List.of("Abstract", "Main body"),
                "Abstract\nThe review compares two procedures.\nMain body\nIt lists studies without a decision framework."
        );
        List<RagflowRetrievedChunk> chunks = List.of(
                new RagflowRetrievedChunk(
                        "人工意见：建议增加研究设计、样本量、主要结局总结表，并说明与既往综述的创新点。",
                        "人工意见-001.docx",
                        0.84
                )
        );

        String prompt = RagflowReviewAugmentationService.buildEnhancedAuditPrompt(
                "Narrative Review",
                document,
                chunks
        );

        assertThat(prompt).contains("历史人工审稿意见");
        assertThat(prompt).contains("不能照抄");
        assertThat(prompt).contains("Major问题");
        assertThat(prompt).contains("Minor问题");
        assertThat(prompt).contains("格式与表述问题");
        assertThat(prompt).contains("研究设计、样本量、主要结局总结表");
        assertThat(prompt).contains("It lists studies without a decision framework");
        assertThat(prompt).contains("只输出合法 JSON 数组");
    }
}
