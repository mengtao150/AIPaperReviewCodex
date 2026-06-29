package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.service.document.ExtractedDocument;
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
}
