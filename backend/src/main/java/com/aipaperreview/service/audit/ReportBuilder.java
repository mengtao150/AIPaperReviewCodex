package com.aipaperreview.service.audit;

import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.domain.StructureTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ReportBuilder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String buildMarkdown(
            String originalFilename,
            String classificationResult,
            Optional<StructureTemplate> structureTemplate,
            String structureAuditResult,
            Optional<ChecklistTemplate> checklistTemplate,
            String checklistAuditResult
    ) {
        return buildMarkdown(
                originalFilename,
                classificationResult,
                structureTemplate,
                structureAuditResult,
                checklistTemplate,
                checklistAuditResult,
                ""
        );
    }

    public String buildMarkdown(
            String originalFilename,
            String classificationResult,
            Optional<StructureTemplate> structureTemplate,
            String structureAuditResult,
            Optional<ChecklistTemplate> checklistTemplate,
            String checklistAuditResult,
            String ragflowEnhancedAuditResult
    ) {
        String reviewType = extractReviewType(classificationResult);
        String structureName = structureTemplate.map(StructureTemplate::getName).orElse("未匹配到结构模板");
        String checklistName = checklistTemplate.map(ChecklistTemplate::getName).orElse("未匹配到审核清单");
        List<ReviewItem> structureItems = extractReviewItems(structureAuditResult, "文章结构");
        List<ReviewItem> checklistItems = extractReviewItems(checklistAuditResult, "审核清单");
        List<ReviewItem> ragflowItems = extractReviewItems(ragflowEnhancedAuditResult, "人工经验增强审查");
        List<ReviewItem> problemItems = new ArrayList<>();
        problemItems.addAll(filterProblemItems(structureItems));
        problemItems.addAll(filterProblemItems(checklistItems));
        problemItems.addAll(filterProblemItems(ragflowItems));

        StringBuilder report = new StringBuilder();
        report.append("# 综述审查报告\n\n");
        report.append("## 一、稿件基本信息\n\n");
        report.append("稿件文件：").append(originalFilename).append("\n\n");
        report.append("判定综述类型：").append(reviewType).append("\n\n");

        report.append("## 二、综述类型判定\n\n");
        report.append("系统根据综述类型判别规则，初步判定该稿件属于：").append(reviewType).append("。\n\n");

        report.append("## 三、文章结构审查结果\n\n");
        report.append("匹配的文章结构模板：").append(structureName).append("\n\n");
        appendItems(report, structureItems, "暂无可读的文章结构审查结果。");

        report.append("## 四、审核清单审查结果\n\n");
        report.append("匹配的审核清单：").append(checklistName).append("\n\n");
        appendItems(report, checklistItems, "暂无可读的审核清单审查结果。");

        int sectionNumber = 5;
        if (!ragflowItems.isEmpty()) {
            report.append("## 五、人工经验增强审查结果\n\n");
            appendItems(report, ragflowItems, "暂无可读的人工经验增强审查结果。");
            sectionNumber = 6;
        }

        report.append("## ").append(chineseSectionNumber(sectionNumber)).append("、缺失内容与修改建议\n\n");
        appendItems(report, problemItems, "当前未发现明确缺失或撰写不完整的项目。");

        report.append("## ").append(chineseSectionNumber(sectionNumber + 1)).append("、总体结论\n\n");
        if (problemItems.isEmpty()) {
            report.append("稿件基本符合当前匹配模板和审核清单的要求，建议结合人工审阅进一步确认细节。\n");
        } else {
            report.append("稿件仍存在若干缺失或撰写不完整的内容，建议作者优先根据上述重点问题逐项补充和修改。\n");
        }
        return report.toString();
    }

    private String chineseSectionNumber(int value) {
        return switch (value) {
            case 5 -> "五";
            case 6 -> "六";
            case 7 -> "七";
            default -> Integer.toString(value);
        };
    }

    private String extractReviewType(String classificationResult) {
        JsonNode root = parse(classificationResult);
        if (root == null || !root.isObject()) {
            return "暂未判定";
        }
        return firstNonBlank(text(root, "review_type"), text(root, "reviewType"), "暂未判定");
    }

    private List<ReviewItem> extractReviewItems(String auditResult, String fallbackItem) {
        JsonNode root = parseAuditResult(auditResult);
        List<ReviewItem> items = new ArrayList<>();
        if (root == null) {
            String fallbackText = readableFallback(auditResult);
            if (!fallbackText.isBlank()) {
                items.add(new ReviewItem(
                        "模型原始审查结果",
                        "无法判断",
                        fallbackText,
                        "模型返回了非标准 JSON，系统已保留原始审查文本。",
                        ""
                ));
            }
            return items;
        }
        if (root.isObject() && root.has("message")) {
            items.add(new ReviewItem(
                    fallbackItem,
                    text(root, "status"),
                    "",
                    text(root, "message"),
                    "请先完善相应模板或清单后重新审查。"
            ));
            return items;
        }
        if (root.isObject()) {
            items.add(toReviewItem(root, fallbackItem));
            return items;
        }
        if (!root.isArray()) {
            return items;
        }
        for (JsonNode node : root) {
            items.add(new ReviewItem(
                    firstNonBlank(text(node, "hierarchy_path"), text(node, "item"), fallbackItem),
                    firstNonBlank(text(node, "status"), "无法判断"),
                    text(node, "evidence"),
                    firstNonBlank(text(node, "reason"), text(node, "comment")),
                    text(node, "suggestion")
            ));
        }
        return items;
    }

    private String readableFallback(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String thinking = extractThinkingText(value);
        String cleaned = value.replaceAll("(?is)<think>.*?</think>", "").strip();
        String visibleText = cleaned;
        int fenceStart = cleaned.indexOf("```");
        int fenceEnd = cleaned.lastIndexOf("```");
        if (fenceStart >= 0 && fenceEnd > fenceStart) {
            int contentStart = cleaned.indexOf('\n', fenceStart);
            if (contentStart >= 0 && contentStart < fenceEnd) {
                visibleText = cleaned.substring(contentStart + 1, fenceEnd).strip();
            }
        }
        if (visibleText.isBlank()) {
            return thinking;
        }
        if (hasUsefulSupplementalAnalysis(thinking)) {
            return visibleText + "\n\n模型补充分析：\n" + thinking;
        }
        return visibleText;
    }

    private String extractThinkingText(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?is)<think>(.*?)</think>").matcher(value);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).strip();
    }

    private boolean hasUsefulSupplementalAnalysis(String thinking) {
        if (thinking == null || thinking.isBlank()) {
            return false;
        }
        String lower = thinking.toLowerCase(java.util.Locale.ROOT);
        return thinking.contains("内容科学性")
                || thinking.contains("逻辑性")
                || thinking.contains("证据支撑")
                || thinking.contains("综述分析深度")
                || thinking.contains("图表与正文呼应")
                || thinking.contains("已撰写")
                || thinking.contains("未撰写")
                || thinking.contains("撰写不完整")
                || lower.contains("for item")
                || lower.contains("status:");
    }

    private ReviewItem toReviewItem(JsonNode node, String fallbackItem) {
        return new ReviewItem(
                firstNonBlank(text(node, "hierarchy_path"), text(node, "item"), fallbackItem),
                firstNonBlank(text(node, "status"), "鏃犳硶鍒ゆ柇"),
                text(node, "evidence"),
                firstNonBlank(text(node, "reason"), text(node, "comment")),
                text(node, "suggestion")
        );
    }

    private List<ReviewItem> filterProblemItems(List<ReviewItem> items) {
        return items.stream()
                .filter(item -> !item.status().contains("已撰写")
                        && !item.status().contains("不适用")
                        && !item.status().contains("no_ragflow_matches")
                        && !item.status().contains("ragflow_error"))
                .toList();
    }

    private void appendItems(StringBuilder report, List<ReviewItem> items, String emptyMessage) {
        if (items.isEmpty()) {
            report.append(emptyMessage).append("\n\n");
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            ReviewItem item = items.get(i);
            report.append(i + 1).append(". ").append(item.item()).append("\n");
            report.append("   - 审查结果：").append(item.status()).append("\n");
            if (!item.evidence().isBlank()) {
                report.append("   - 依据：").append(item.evidence()).append("\n");
            }
            if (!item.reason().isBlank()) {
                report.append("   - 问题说明：").append(item.reason()).append("\n");
            }
            if (!item.suggestion().isBlank()) {
                report.append("   - 修改建议：").append(item.suggestion()).append("\n");
            }
            report.append("\n");
        }
    }

    private JsonNode parse(String value) {
        return LlmJsonPayloadExtractor.extract(OBJECT_MAPPER, value).orElse(null);
    }

    private JsonNode parseAuditResult(String value) {
        return LlmJsonPayloadExtractor.extractComplete(OBJECT_MAPPER, value).orElse(null);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? "" : value.asText("").strip();
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private record ReviewItem(String item, String status, String evidence, String reason, String suggestion) {
    }
}
