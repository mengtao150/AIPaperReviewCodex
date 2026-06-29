package com.aipaperreview.service.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LibraryImportServiceTest {
    @Test
    void structurePromptRequiresHeadingsAndSubheadingsAsAuditItems() {
        String prompt = LibraryImportService.buildStructureParsePrompt(
                "Narrative Review",
                "1. Introduction\n1.1 Background\n1.2 Rationale and knowledge gap"
        );

        assertThat(prompt).contains("children");
        assertThat(prompt).contains("sub_items");
        assertThat(prompt).contains("每一个标题下面的小标题");
        assertThat(prompt).contains("都必须作为后续结构审查项");
    }
}
