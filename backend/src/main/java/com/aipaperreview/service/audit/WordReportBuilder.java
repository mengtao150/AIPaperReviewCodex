package com.aipaperreview.service.audit;

import com.aipaperreview.domain.AuditJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Component;

@Component
public class WordReportBuilder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public byte[] build(AuditJob job) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePage(document);
            if (hasAuditFields(job)) {
                appendReviewerReport(document, job);
            } else {
                String markdown = job.getFinalReport();
                if (markdown == null || markdown.isBlank()) {
                    markdown = "# 综述稿件审查报告\n\n暂无报告内容。";
                }
                appendMarkdown(document, markdown);
            }
            document.write(output);
            return output.toByteArray();
        }
    }

    private void configurePage(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(12240));
        pageSize.setH(BigInteger.valueOf(15840));
        CTPageMar margins = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1440));
        margins.setRight(BigInteger.valueOf(1440));
        margins.setBottom(BigInteger.valueOf(1440));
        margins.setLeft(BigInteger.valueOf(1440));
    }

    private boolean hasAuditFields(AuditJob job) {
        return hasText(job.getClassificationResult())
                || hasText(job.getStructureAuditResult())
                || hasText(job.getChecklistAuditResult())
                || hasText(job.getRagflowEnhancedAuditResult());
    }

    private void appendReviewerReport(XWPFDocument document, AuditJob job) {
        String reviewType = extractReviewType(job);
        List<ReviewItem> structureItems = extractReviewItems(job.getStructureAuditResult(), "文章结构审查");
        List<ReviewItem> checklistItems = extractReviewItems(job.getChecklistAuditResult(), "审核清单审查");
        List<ReviewItem> ragflowItems = extractReviewItems(job.getRagflowEnhancedAuditResult(), "人工经验增强审查");
        List<ReviewItem> structureProblems = filterProblemItems(structureItems);
        List<ReviewItem> checklistProblems = filterProblemItems(checklistItems);
        List<ReviewItem> ragflowProblems = filterProblemItems(ragflowItems);
        List<ReviewItem> problemItems = new ArrayList<>();
        problemItems.addAll(structureProblems);
        problemItems.addAll(checklistProblems);
        problemItems.addAll(ragflowProblems);

        addTitle(document, "综述稿件审查意见书");
        addSubtitle(document, "面向审稿人的问题导向修改意见");

        addSectionHeading(document, "一、稿件基本信息");
        addLabeledParagraph(document, "稿件文件", blankToDefault(job.getOriginalFilename(), "未命名稿件"));
        addLabeledParagraph(document, "审查状态", humanStatus(job.getStatus()));
        addLabeledParagraph(document, "判定综述类型", reviewType);
        if (job.getCreatedAt() != null) {
            addLabeledParagraph(document, "创建时间", DATE_TIME_FORMATTER.format(job.getCreatedAt()));
        }

        addSectionHeading(document, "二、审查结论摘要");
        appendSummary(document, reviewType, problemItems);

        addSectionHeading(document, "三、重点问题概览");
        appendIssueOverview(document, problemItems);

        addSectionHeading(document, "四、文章结构审查结果");
        appendIssueItems(document, structureProblems, "未发现需要审稿人重点关注的文章结构问题。");

        addSectionHeading(document, "五、审核清单审查结果");
        appendIssueItems(document, checklistProblems, "未发现需要审稿人重点关注的审核清单问题。");

        int sectionNumber = 6;
        if (!ragflowProblems.isEmpty()) {
            addSectionHeading(document, "六、人工经验增强审查结果");
            appendIssueItems(document, ragflowProblems, "未检索到可用于本稿件的人工经验增强审查意见。");
            sectionNumber = 7;
        }

        addSectionHeading(document, chineseSectionNumber(sectionNumber) + "、总体结论");
        if (problemItems.isEmpty()) {
            addParagraph(document, "稿件基本符合当前匹配模板和审核清单要求，建议审稿人结合学科判断进一步确认细节。", 11, false, ParagraphAlignment.LEFT, false);
        } else {
            addParagraph(document, "稿件仍存在若干缺失或撰写不完整的内容，建议优先根据上述重点问题逐项修改，并重点核对结构完整性、证据支撑和正文逻辑。", 11, false, ParagraphAlignment.LEFT, false);
        }
    }

    private void appendSummary(XWPFDocument document, String reviewType, List<ReviewItem> problemItems) {
        addLabeledParagraph(document, "综述类型", reviewType);
        addLabeledParagraph(document, "重点问题数量", problemItems.size() + " 项");
        if (problemItems.isEmpty()) {
            addParagraph(document, "当前自动审查未提取到需要优先修改的问题。建议审稿人结合学科判断复核关键结论和证据链。", 11, false, ParagraphAlignment.LEFT, false);
            return;
        }
        addParagraph(document, "以下意见仅保留需要审稿人关注的问题项，已隐藏已撰写、不适用和原始机器输出内容。", 11, false, ParagraphAlignment.LEFT, false);
    }

    private void appendIssueOverview(XWPFDocument document, List<ReviewItem> problemItems) {
        if (problemItems.isEmpty()) {
            addParagraph(document, "当前未发现明确缺失或撰写不完整的项目。", 11, false, ParagraphAlignment.LEFT, false);
            return;
        }
        for (int index = 0; index < problemItems.size(); index++) {
            ReviewItem item = problemItems.get(index);
            addParagraph(document, (index + 1) + ". " + safeText(item.item(), 120) + "（" + safeText(item.status(), 40) + "）", 11, false, ParagraphAlignment.LEFT, false);
        }
    }

    private void appendIssueItems(XWPFDocument document, List<ReviewItem> items, String emptyMessage) {
        if (items.isEmpty()) {
            addParagraph(document, emptyMessage, 11, false, ParagraphAlignment.LEFT, false);
            return;
        }
        for (int index = 0; index < items.size(); index++) {
            ReviewItem item = items.get(index);
            addIssueTitle(document, (index + 1) + ". " + safeText(item.item(), 140));
            addLabeledParagraph(document, "问题级别", safeText(item.status(), 40));
            addLabeledParagraph(document, "问题位置", safeText(firstNonBlank(item.suggestedLocation(), item.item()), 120));
            addIfPresent(document, "问题说明", safeText(item.reason(), 260));
            addIfPresent(document, "修改建议", safeText(item.suggestion(), 260));
            addIfPresent(document, "原文依据短摘", safeText(item.evidence(), 110));
        }
    }

    private String chineseSectionNumber(int value) {
        return switch (value) {
            case 5 -> "五";
            case 6 -> "六";
            case 7 -> "七";
            default -> Integer.toString(value);
        };
    }

    private String extractReviewType(AuditJob job) {
        JsonNode root = parseAnyJson(job.getClassificationResult());
        String fromJson = root == null ? "" : firstNonBlank(text(root, "review_type"), text(root, "reviewType"));
        if (!fromJson.isBlank()) {
            return fromJson;
        }
        if (job.getDetectedReviewType() != null && hasText(job.getDetectedReviewType().getName())) {
            return job.getDetectedReviewType().getName();
        }
        return "暂未判定";
    }

    private void appendClassificationDetails(XWPFDocument document, String classificationResult) {
        JsonNode root = parseAnyJson(classificationResult);
        if (root == null || !root.isObject()) {
            return;
        }
        String reason = firstNonBlank(text(root, "reason"), text(root, "rationale"), text(root, "explanation"));
        String evidence = firstNonBlank(text(root, "evidence"), text(root, "key_evidence"), text(root, "confidence"));
        if (!reason.isBlank()) {
            addLabeledParagraph(document, "判定依据", reason);
        }
        if (!evidence.isBlank()) {
            addLabeledParagraph(document, "补充说明", evidence);
        }
    }

    private List<ReviewItem> extractReviewItems(String value, String fallbackItem) {
        List<ReviewItem> items = new ArrayList<>();
        if (!hasText(value)) {
            return items;
        }

        JsonNode root = LlmJsonPayloadExtractor.extractComplete(OBJECT_MAPPER, value).orElse(null);
        if (root != null) {
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.isObject() && !isEmptyAuditObject(node)) {
                        items.add(toReviewItem(node, fallbackItem));
                    }
                }
                return items;
            }
            if (root.isObject() && !isEmptyAuditObject(root)) {
                items.add(toReviewItem(root, fallbackItem));
                return items;
            }
            if (root.isObject()) {
                return items;
            }
        }

        for (String objectCandidate : extractObjectCandidates(value)) {
            parseObject(objectCandidate).ifPresent(node -> items.add(toReviewItem(node, fallbackItem)));
        }

        return items;
    }

    private ReviewItem toReviewItem(JsonNode node, String fallbackItem) {
        return new ReviewItem(
                firstNonBlank(text(node, "hierarchy_path"), text(node, "item"), text(node, "requirement"), fallbackItem),
                firstNonBlank(text(node, "status"), "无法判断"),
                text(node, "evidence"),
                firstNonBlank(text(node, "reason"), text(node, "comment")),
                text(node, "suggestion"),
                firstNonBlank(text(node, "suggested_location"), text(node, "suggestedLocation"))
        );
    }

    private boolean isEmptyAuditObject(JsonNode node) {
        return node == null || node.size() == 0 || (
                !hasText(text(node, "hierarchy_path"))
                        && !hasText(text(node, "item"))
                        && !hasText(text(node, "requirement"))
                        && !hasText(text(node, "status"))
                        && !hasText(text(node, "evidence"))
                        && !hasText(text(node, "reason"))
                        && !hasText(text(node, "comment"))
                        && !hasText(text(node, "suggestion"))
                        && !hasText(text(node, "message"))
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

    private void appendMarkdown(XWPFDocument document, String markdown) {
        boolean codeBlock = false;
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.stripTrailing();
            if (line.strip().startsWith("```")) {
                codeBlock = !codeBlock;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            if (!codeBlock && line.startsWith("# ")) {
                addParagraph(document, line.substring(2).trim(), 18, true, ParagraphAlignment.CENTER, false);
            } else if (!codeBlock && line.startsWith("## ")) {
                addParagraph(document, line.substring(3).trim(), 14, true, ParagraphAlignment.LEFT, false);
            } else {
                addParagraph(document, line, 11, false, ParagraphAlignment.LEFT, codeBlock);
            }
        }
    }

    private void addIfPresent(XWPFDocument document, String label, String value) {
        if (hasText(value)) {
            addLabeledParagraph(document, label, value);
        }
    }

    private void addLabeledParagraph(XWPFDocument document, String label, String value) {
        addParagraph(document, label + "：" + blankToDefault(value, "无"), 11, false, ParagraphAlignment.LEFT, false);
    }

    private void addTitle(XWPFDocument document, String text) {
        addParagraph(document, text, 18, true, ParagraphAlignment.CENTER, false, 0, 120);
    }

    private void addSubtitle(XWPFDocument document, String text) {
        addParagraph(document, text, 10, false, ParagraphAlignment.CENTER, false, 0, 260);
    }

    private void addSectionHeading(XWPFDocument document, String text) {
        addParagraph(document, text, 13, true, ParagraphAlignment.LEFT, false, 240, 120);
    }

    private void addIssueTitle(XWPFDocument document, String text) {
        addParagraph(document, text, 11, true, ParagraphAlignment.LEFT, false, 140, 80);
    }

    private void addParagraph(
            XWPFDocument document,
            String text,
            int fontSize,
            boolean bold,
            ParagraphAlignment alignment,
            boolean monospace
    ) {
        addParagraph(document, text, fontSize, bold, alignment, monospace, 0, 120);
    }

    private void addParagraph(
            XWPFDocument document,
            String text,
            int fontSize,
            boolean bold,
            ParagraphAlignment alignment,
            boolean monospace,
            int spacingBefore,
            int spacingAfter
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setSpacingBetween(1.15);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontFamily("Microsoft YaHei");
    }

    private JsonNode parseAnyJson(String value) {
        return LlmJsonPayloadExtractor.extract(OBJECT_MAPPER, value).orElse(null);
    }

    private java.util.Optional<JsonNode> parseObject(String value) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(value);
            return node.isObject() ? java.util.Optional.of(node) : java.util.Optional.empty();
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }
    }

    private List<String> extractObjectCandidates(String value) {
        List<String> candidates = new ArrayList<>();
        if (!hasText(value)) {
            return candidates;
        }
        for (int start = 0; start < value.length(); start++) {
            if (value.charAt(start) != '{') {
                continue;
            }
            int end = balancedObjectEnd(value, start);
            if (end > start) {
                candidates.add(value.substring(start, end + 1));
                start = end;
            }
        }
        return candidates;
    }

    private int balancedObjectEnd(String value, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? "" : value.asText("").strip();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (hasText(value)) {
                return value.strip();
            }
        }
        return "";
    }

    private String blankToDefault(String value, String fallback) {
        return hasText(value) ? value.strip() : fallback;
    }

    private String safeText(String value, int maxChars) {
        if (!hasText(value)) {
            return "";
        }
        String cleaned = value
                .replaceAll("(?is)<think>.*?</think>", "")
                .replace("```json", "")
                .replace("```", "")
                .replaceAll("\\s+", " ")
                .strip();
        if (cleaned.length() <= maxChars) {
            return cleaned;
        }
        return cleaned.substring(0, maxChars).strip() + "……";
    }

    private String humanStatus(AuditJob.Status status) {
        if (status == null) {
            return "未记录";
        }
        return switch (status) {
            case UPLOADED -> "已上传";
            case EXTRACTED -> "已解析稿件";
            case CLASSIFIED -> "已完成类型判定";
            case STRUCTURE_AUDITED -> "已完成结构审查";
            case CHECKLIST_AUDITED -> "已完成审核清单审查";
            case COMPLETED -> "审查完成";
            case FAILED -> "审查失败";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ReviewItem(
            String item,
            String status,
            String evidence,
            String reason,
            String suggestion,
            String suggestedLocation
    ) {
    }
}
