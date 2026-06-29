package com.aipaperreview.service.audit;

import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.domain.StructureTemplate;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ReportBuilder {
    public String buildMarkdown(
            String originalFilename,
            String classificationResult,
            Optional<StructureTemplate> structureTemplate,
            String structureAuditResult,
            Optional<ChecklistTemplate> checklistTemplate,
            String checklistAuditResult
    ) {
        String structureName = structureTemplate.map(StructureTemplate::getName).orElse("未匹配到结构模板");
        String checklistName = checklistTemplate.map(ChecklistTemplate::getName).orElse("未匹配到审核清单");
        return """
                # 综述稿件审查报告

                ## 稿件

                %s

                ## 类型判别结果

                ```json
                %s
                ```

                ## 匹配文章结构模板

                %s

                ## 结构审查结果

                ```json
                %s
                ```

                ## 匹配审核清单

                %s

                ## 清单逐项审查结果

                ```json
                %s
                ```
                """.formatted(
                originalFilename,
                emptyJson(classificationResult),
                structureName,
                emptyJson(structureAuditResult),
                checklistName,
                emptyJson(checklistAuditResult)
        );
    }

    private String emptyJson(String value) {
        return value == null || value.isBlank() ? "{}" : value;
    }
}
