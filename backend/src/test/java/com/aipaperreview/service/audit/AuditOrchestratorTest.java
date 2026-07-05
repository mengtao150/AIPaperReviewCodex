package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.ChecklistItem;
import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.service.document.ExtractedDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditOrchestratorTest {
    @Test
    void structureAuditPromptIncludesNestedHeadingsAsIndependentItems() {
        StructureTemplate template = new StructureTemplate();
        template.setParsedSections("""
                [
                  {
                    "title": "Introduction",
                    "children": [
                      {"title": "Background"},
                      {"title": "Knowledge gap"}
                    ]
                  }
                ]
                """);
        template.setRequiredItems("""
                [
                  {"item": "Explain background and knowledge gap"}
                ]
                """);
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.docx",
                "Narrative review manuscript",
                List.of("Introduction", "Background text"),
                "Introduction\nBackground text"
        );

        String prompt = AuditOrchestrator.buildStructureAuditPrompt(document, template);

        assertThat(prompt).contains("parsed_sections");
        assertThat(prompt).contains("children/sub_items");
        assertThat(prompt).contains("每一个标题下面的小标题");
        assertThat(prompt).contains("独立结构审查项");
        assertThat(prompt).contains("hierarchy_path");
        assertThat(prompt).contains("Knowledge gap");
    }

    @Test
    void structureAuditPromptFlattensEveryTemplateNodeIntoRequiredAuditItems() {
        StructureTemplate template = new StructureTemplate();
        template.setParsedSections("""
                [
                  {
                    "name": "Introduction",
                    "requirement": "Introduce the topic",
                    "children": [
                      {
                        "name": "Background",
                        "requirement": "Explain context",
                        "sub_items": [
                          {"name": "Knowledge gap", "requirement": "State the gap"}
                        ]
                      }
                    ]
                  },
                  {
                    "name": "Main body",
                    "children": [
                      {"name": "Strengths and limitations", "requirement": "Discuss both"}
                    ]
                  }
                ]
                """);
        template.setRequiredItems("""
                [
                  {"name": "Abstract", "requirement": "200-350 words"}
                ]
                """);
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.docx",
                "Narrative review manuscript",
                List.of("Introduction", "Main body"),
                "Introduction\nMain body"
        );

        String prompt = AuditOrchestrator.buildStructureAuditPrompt(document, template);

        assertThat(prompt).contains("必须对下面结构审查清单中的每一项输出一个 JSON 数组元素");
        assertThat(prompt).contains("1. Abstract");
        assertThat(prompt).contains("2. Introduction");
        assertThat(prompt).contains("3. Introduction > Background");
        assertThat(prompt).contains("4. Introduction > Background > Knowledge gap");
        assertThat(prompt).contains("5. Main body");
        assertThat(prompt).contains("6. Main body > Strengths and limitations");
        assertThat(prompt).contains("必须用中文输出字段内容");
    }

    @Test
    void structureAuditPromptExpandsPlainTextTemplateRulesFromLibrary() {
        StructureTemplate template = new StructureTemplate();
        template.setParsedSections("Title Page; Abstract; Keywords; Introduction");
        template.setRequiredItems("""
                Title Page: title, authors, affiliation, correspondence, running title, word count, contributions.
                Abstract: unstructured, 200-350 words, accurately describes article content.
                Introduction: Background; Rationale and knowledge gap; Objective; checklist statement if applicable.
                """);
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.pdf",
                "Narrative review manuscript",
                List.of("Abstract", "Introduction"),
                "Abstract\nIntroduction"
        );

        String prompt = AuditOrchestrator.buildStructureAuditPrompt(document, template);

        assertThat(prompt).contains("1. Title Page");
        assertThat(prompt).contains("2. Title Page > title");
        assertThat(prompt).contains("3. Title Page > authors");
        assertThat(prompt).contains("9. Abstract");
        assertThat(prompt).contains("10. Abstract > unstructured");
        assertThat(prompt).contains("13. Introduction");
        assertThat(prompt).contains("14. Introduction > Background");
        assertThat(prompt).contains("16. Introduction > Objective");
        assertThat(prompt).contains("18. Keywords");
    }

    @Test
    void checklistAuditPromptRequiresSpecificScientificAndLogicalAnalysisForMainContent() {
        ChecklistItem item = new ChecklistItem();
        item.setOrderIndex(13);
        item.setCategory("主要内容");
        item.setRequirement("概述文献结果，评价纳入文献局限性和/或质量，总结综述优势与局限，提出未来研究想法，并总结图表。");
        item.setEvaluationGuidance("检查主体是否不只是罗列文献，而有分析和评价。");
        ExtractedDocument document = new ExtractedDocument(
                "manuscript.pdf",
                "Narrative review manuscript",
                List.of("Main Body", "Ductal Clearance and Efficacy"),
                "Main Body\nDuctal Clearance and Efficacy\nComplications and Safety\nStrengths and Limitations"
        );

        String prompt = AuditOrchestrator.buildChecklistAuditPrompt(List.of(item), document);

        assertThat(prompt).contains("主要内容专项审查要求");
        assertThat(prompt).contains("内容科学性");
        assertThat(prompt).contains("逻辑性");
        assertThat(prompt).contains("证据支撑");
        assertThat(prompt).contains("不能只给笼统结论");
        assertThat(prompt).contains("引用稿件中的具体章节、段落或关键表述");
        assertThat(prompt).contains("reason 必须按【内容科学性】【逻辑性】【证据支撑】【综述分析深度】【图表与正文呼应】五个维度分别给出判断");
    }

    @Test
    void auditPayloadNormalizationKeepsMixedObjectSequenceRaw() {
        String response = """
                <think>For item 13 (主要内容): 内容科学性 and 逻辑性 need detailed review.</think>
                {"item":"题目","status":"已撰写"}
                {"item":"摘要","status":"已撰写"}
                """;

        String normalized = AuditOrchestrator.normalizeAuditPayload(new ObjectMapper(), response);

        assertThat(normalized).isEqualTo(response);
        assertThat(normalized).contains("For item 13 (主要内容)");
        assertThat(normalized).contains("\"摘要\"");
    }

    @Test
    void auditPayloadNormalizationCompactsCompleteArray() {
        String response = """
                ```json
                [
                  {"item":"题目","status":"已撰写"},
                  {"item":"摘要","status":"撰写不完整"}
                ]
                ```
                """;

        String normalized = AuditOrchestrator.normalizeAuditPayload(new ObjectMapper(), response);

        assertThat(normalized).isEqualTo("[{\"item\":\"题目\",\"status\":\"已撰写\"},{\"item\":\"摘要\",\"status\":\"撰写不完整\"}]");
    }
}
