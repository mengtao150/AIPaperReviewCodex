package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReportBuilderTest {
    @Test
    void buildsStructuredReviewAuditReportWithoutJsonBlocks() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                """
                        [
                          {
                            "item": "Introduction - Rationale and knowledge gap",
                            "status": "未撰写",
                            "reason": "未说明开展该综述的必要性。",
                            "suggestion": "请在引言中补充研究背景、争议点和知识空白。"
                          }
                        ]
                        """,
                Optional.empty(),
                """
                        [
                          {
                            "item": "Abstract - Methods",
                            "status": "撰写不完整",
                            "evidence": "摘要方法部分未说明语言限制。",
                            "suggestion": "请补充文献检索语言限制。"
                          }
                        ]
                        """
        );

        assertThat(markdown).contains("# 综述审查报告");
        assertThat(markdown).contains("## 一、稿件基本信息");
        assertThat(markdown).contains("## 二、综述类型判定");
        assertThat(markdown).contains("## 三、文章结构审查结果");
        assertThat(markdown).contains("Introduction - Rationale and knowledge gap");
        assertThat(markdown).contains("请在引言中补充研究背景、争议点和知识空白");
        assertThat(markdown).contains("## 四、审核清单审查结果");
        assertThat(markdown).contains("Abstract - Methods");
        assertThat(markdown).contains("## 五、缺失内容与修改建议");
        assertThat(markdown).contains("## 六、总体结论");
        assertThat(markdown).doesNotContain("```json");
        assertThat(markdown).doesNotContain("\"review_type\"");
    }

    @Test
    void extractsAuditItemsFromReasoningWrappedJsonBlock() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                """
                        <think>model reasoning</think>
                        ```json
                        {"review_type":"Narrative Review"}
                        ```
                        """,
                Optional.empty(),
                """
                        <think>I checked the template.</think>
                        ```json
                        [
                          {
                            "item": "Introduction - Objective",
                            "hierarchy_path": "Introduction > Objective",
                            "status": "未撰写",
                            "reason": "Manuscript has no explicit objective.",
                            "suggestion": "Add a clear objective at the end of Introduction."
                          }
                        ]
                        ```
                        """,
                Optional.empty(),
                "[]"
        );

        assertThat(markdown).contains("Narrative Review");
        assertThat(markdown).contains("Introduction > Objective");
        assertThat(markdown).contains("Manuscript has no explicit objective.");
        assertThat(markdown).doesNotContain("暂无可读的文章结构审查结果");
    }

    @Test
    void treatsSingleAuditObjectAsOneReadableItem() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                """
                        {
                          "item": "Title Page",
                          "hierarchy_path": "Title Page",
                          "status": "撰写不完整",
                          "reason": "Missing authors and affiliations.",
                          "suggestion": "Add a complete title page."
                        }
                        """,
                Optional.empty(),
                """
                        {
                          "item": "Checklist - Title",
                          "status": "撰写不完整",
                          "reason": "Search period is not in the title.",
                          "suggestion": "Add the search period."
                        }
                        """
        );

        assertThat(markdown).contains("Title Page");
        assertThat(markdown).contains("Missing authors and affiliations.");
        assertThat(markdown).contains("Checklist - Title");
        assertThat(markdown).contains("Search period is not in the title.");
        assertThat(markdown).doesNotContain("暂无可读的文章结构审查结果");
        assertThat(markdown).doesNotContain("暂无可读的审核清单审查结果");
    }

    @Test
    void fallsBackToRawAuditTextWhenModelReturnsInvalidJson() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                """
                        <think>reasoning should be hidden</think>
                        ```json
                        [
                          {"item":"Title Page","status":"撰写不完整","evidence":"Title says "A Narrative Review""}
                        ]
                        ```
                        """,
                Optional.empty(),
                "[]"
        );

        assertThat(markdown).contains("模型原始审查结果");
        assertThat(markdown).contains("Title Page");
        assertThat(markdown).doesNotContain("reasoning should be hidden");
        assertThat(markdown).doesNotContain("暂无可读的文章结构审查结果");
    }

    @Test
    void fallsBackToThinkingTextWhenModelReturnsOnlyReasoning() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                "<think>Title Page is missing authors and affiliations.</think>",
                Optional.empty(),
                "[]"
        );

        assertThat(markdown).contains("模型原始审查结果");
        assertThat(markdown).contains("Title Page is missing authors and affiliations.");
        assertThat(markdown).doesNotContain("暂无可读的文章结构审查结果");
    }

    @Test
    void keepsUsefulSupplementalAnalysisWhenInvalidJsonIsOnlyPartial() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                "[]",
                Optional.empty(),
                """
                        <think>
                        For item 13 (主要内容):
                        - 内容科学性: Ductal clearance and pancreatitis claims are supported by cited comparative studies.
                        - 逻辑性: Main body follows efficacy, safety, cost, implementation barriers, and patient perspective.
                        - 证据支撑: Some quantitative claims need closer citation numbers.
                        </think>
                        ```json
                        [
                          {"item":"[摘要] Key Content and Findings","status":"已撰写","evidence":"LCBDE 88-100%"}
                        ```
                        """
        );

        assertThat(markdown).contains("[摘要] Key Content and Findings");
        assertThat(markdown).contains("模型补充分析");
        assertThat(markdown).contains("For item 13 (主要内容)");
        assertThat(markdown).contains("内容科学性");
        assertThat(markdown).contains("逻辑性");
        assertThat(markdown).contains("证据支撑");
    }

    @Test
    void includesRagflowEnhancedAuditSectionWhenAvailable() {
        ReportBuilder builder = new ReportBuilder();

        String markdown = builder.buildMarkdown(
                "manuscript.docx",
                "{\"review_type\":\"Narrative Review\"}",
                Optional.empty(),
                "[]",
                Optional.empty(),
                "[]",
                """
                        [
                          {
                            "item": "Major问题：缺少研究设计和样本量总结表",
                            "status": "Major问题",
                            "evidence": "正文仅叙述不同研究结果，未汇总研究设计、样本量和主要结局。",
                            "reason": "历史人工审稿案例提示该类综述应提供关键研究特征表；当前稿件未满足。",
                            "suggestion": "增加研究设计、样本量、干预方式和主要结局的总结表。"
                          }
                        ]
                        """
        );

        assertThat(markdown).contains("## 五、人工经验增强审查结果");
        assertThat(markdown).contains("缺少研究设计和样本量总结表");
        assertThat(markdown).contains("## 六、缺失内容与修改建议");
        assertThat(markdown).contains("## 七、总体结论");
    }
}
