package com.aipaperreview.service.audit;

import com.aipaperreview.domain.AuditJob;
import com.aipaperreview.domain.ChecklistItem;
import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.domain.ReviewType;
import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.repository.AuditJobRepository;
import com.aipaperreview.repository.ChecklistItemRepository;
import com.aipaperreview.repository.ReviewTypeRepository;
import com.aipaperreview.service.document.DocumentExtractor;
import com.aipaperreview.service.document.ExtractedDocument;
import com.aipaperreview.service.llm.LlmClient;
import com.aipaperreview.service.llm.LlmProviderService;
import com.aipaperreview.service.llm.LlmRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.task.TaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuditOrchestrator {
    private final List<DocumentExtractor> extractors;
    private final AuditJobRepository auditJobRepository;
    private final ReviewTypeRepository reviewTypeRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ClassificationRuleProvider classificationRuleProvider;
    private final TemplateMatcher templateMatcher;
    private final ChecklistMatcher checklistMatcher;
    private final LlmProviderService llmProviderService;
    private final LlmClient llmClient;
    private final ReportBuilder reportBuilder;
    private final RagflowReviewAugmentationService ragflowReviewAugmentationService;
    private final ManuscriptParagraphService manuscriptParagraphService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor auditTaskExecutor;
    private final Path storageDir;

    public AuditOrchestrator(
            List<DocumentExtractor> extractors,
            AuditJobRepository auditJobRepository,
            ReviewTypeRepository reviewTypeRepository,
            ChecklistItemRepository checklistItemRepository,
            ClassificationRuleProvider classificationRuleProvider,
            TemplateMatcher templateMatcher,
            ChecklistMatcher checklistMatcher,
            LlmProviderService llmProviderService,
            LlmClient llmClient,
            ReportBuilder reportBuilder,
            RagflowReviewAugmentationService ragflowReviewAugmentationService,
            ManuscriptParagraphService manuscriptParagraphService,
            ObjectMapper objectMapper,
            TaskExecutor auditTaskExecutor,
            @Value("${app.storage-dir}") String storageDir
    ) {
        this.extractors = extractors;
        this.auditJobRepository = auditJobRepository;
        this.reviewTypeRepository = reviewTypeRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.classificationRuleProvider = classificationRuleProvider;
        this.templateMatcher = templateMatcher;
        this.checklistMatcher = checklistMatcher;
        this.llmProviderService = llmProviderService;
        this.llmClient = llmClient;
        this.reportBuilder = reportBuilder;
        this.ragflowReviewAugmentationService = ragflowReviewAugmentationService;
        this.manuscriptParagraphService = manuscriptParagraphService;
        this.objectMapper = objectMapper;
        this.auditTaskExecutor = auditTaskExecutor;
        this.storageDir = Path.of(storageDir);
    }

    public AuditJob runAudit(MultipartFile file) throws IOException {
        AuditJob job = createAuditJob(file);
        return processAudit(job.getId());
    }

    public AuditJob startAudit(MultipartFile file) throws IOException {
        AuditJob job = createAuditJob(file);
        auditTaskExecutor.execute(() -> processAudit(job.getId()));
        return job;
    }

    private AuditJob createAuditJob(MultipartFile file) throws IOException {
        Files.createDirectories(storageDir);
        String originalFilename = file.getOriginalFilename() == null ? "manuscript.docx" : file.getOriginalFilename();
        Path savedPath = storageDir.resolve(UUID.randomUUID() + "-" + originalFilename);
        file.transferTo(savedPath);

        AuditJob job = new AuditJob();
        job.setOriginalFilename(originalFilename);
        job.setManuscriptFilePath(savedPath.toString());
        job = auditJobRepository.save(job);
        return job;
    }

    private AuditJob processAudit(Long jobId) {
        AuditJob job = auditJobRepository.findById(jobId).orElseThrow();
        try {
            String originalFilename = job.getOriginalFilename();
            DocumentExtractor extractor = findExtractor(originalFilename);
            ExtractedDocument document;
            try (InputStream inputStream = Files.newInputStream(Path.of(job.getManuscriptFilePath()))) {
                document = extractor.extract(originalFilename, inputStream);
            }
            job.setManuscriptParagraphsJson(manuscriptParagraphService.toJson(document.paragraphs()));
            job.setStatus(AuditJob.Status.EXTRACTED);
            auditJobRepository.save(job);

            LlmProviderConfig provider = llmProviderService.activeProvider();
            String classification = normalizeJsonPayload(classify(provider, document));
            job.setClassificationResult(classification);
            String reviewTypeName = extractReviewTypeName(classification).orElse("Narrative Review");
            ReviewType reviewType = reviewTypeRepository.findByNameIgnoreCaseAndActiveTrue(reviewTypeName)
                    .orElseGet(() -> reviewTypeRepository.findByNameIgnoreCaseAndActiveTrue("Narrative Review").orElse(null));
            job.setDetectedReviewType(reviewType);
            job.setStatus(AuditJob.Status.CLASSIFIED);
            auditJobRepository.save(job);

            Optional<StructureTemplate> structureTemplate = templateMatcher.match(reviewTypeName);
            String structureAudit = normalizeAuditPayload(objectMapper, auditStructure(provider, document, structureTemplate));
            job.setStructureAuditResult(structureAudit);
            job.setStatus(AuditJob.Status.STRUCTURE_AUDITED);
            auditJobRepository.save(job);

            Optional<ChecklistTemplate> checklistTemplate = checklistMatcher.match(reviewTypeName);
            String checklistAudit = normalizeAuditPayload(objectMapper, auditChecklist(provider, document, checklistTemplate));
            job.setChecklistAuditResult(checklistAudit);
            job.setStatus(AuditJob.Status.CHECKLIST_AUDITED);
            auditJobRepository.save(job);

            String ragflowEnhancedAudit = ragflowReviewAugmentationService.audit(provider, reviewTypeName, document)
                    .map(value -> normalizeAuditPayload(objectMapper, value))
                    .orElse("");
            job.setRagflowEnhancedAuditResult(ragflowEnhancedAudit);

            String report = reportBuilder.buildMarkdown(
                    originalFilename,
                    classification,
                    structureTemplate,
                    structureAudit,
                    checklistTemplate,
                    checklistAudit,
                    ragflowEnhancedAudit
            );
            job.setFinalReport(report);
            job.setStatus(AuditJob.Status.COMPLETED);
            return auditJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(AuditJob.Status.FAILED);
            job.setErrorMessage(ex.getMessage());
            return auditJobRepository.save(job);
        }
    }

    private DocumentExtractor findExtractor(String filename) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("暂不支持该文件格式，请上传 .docx 或 .pdf 稿件"));
    }

    private String classify(LlmProviderConfig provider, ExtractedDocument document) {
        String prompt = """
                请阅读以下稿件全文，并严格按照 Review 类型判别准则判断文章类型。

                稿件标题：%s

                稿件全文：
                %s
                """.formatted(document.title(), document.fullText());
        return llmClient.complete(provider, new LlmRequest(classificationRuleProvider.reviewTypeFlowchartRules(), prompt)).content();
    }

    private String auditStructure(LlmProviderConfig provider, ExtractedDocument document, Optional<StructureTemplate> template) {
        if (template.isEmpty()) {
            return """
                    {"status":"missing_template","message":"未找到该综述类型对应的文章结构模板"}
                    """;
        }
        String prompt = buildStructureAuditPrompt(document, template.get());
        return llmClient.complete(provider, new LlmRequest("你是医学期刊综述稿件结构审查助手。", prompt)).content();
    }

    static String buildStructureAuditPrompt(ExtractedDocument document, StructureTemplate template) {
        String auditItems = buildStructureAuditItems(template);
        return """
                请根据文章结构模板检查稿件是否按模板撰写。
                审查时必须同时检查：
                1. 一级标题是否存在、顺序是否合理；
                2. 每一个标题下面的小标题、子标题或子项是否存在；
                3. 每个标题或小标题对应的内容是否写到位。

                请把 parsed_sections 中的 children/sub_items 也作为独立结构审查项，不要只检查一级标题。
                必须对下面结构审查清单中的每一项输出一个 JSON 数组元素，不得遗漏，也不得只输出摘要相关项目。
                逐项输出 JSON 数组，每项包含 item, hierarchy_path, status, evidence, reason, suggestion。
                status 只能使用：已撰写、未撰写、撰写不完整、无法判断、不适用。
                只输出合法 JSON 数组，不要输出思考过程，不要输出 Markdown 代码块。
                必须用中文输出字段内容。
                字段值中不要使用英文双引号引用原文；如需引用原文，请改用中文单引号或概述原文。

                结构审查清单（每一行都必须检查并输出）：
                %s

                文章结构模板 parsed_sections（包含标题及小标题层级）：
                %s

                文章结构模板 required_items（必需结构项）：
                %s

                稿件全文：
                %s
                """.formatted(
                auditItems,
                nullToBlank(template.getParsedSections()),
                nullToBlank(template.getRequiredItems()),
                document.fullText()
        );
    }

    private static String buildStructureAuditItems(StructureTemplate template) {
        List<StructureAuditItem> items = new ArrayList<>();
        Set<String> seenPaths = new LinkedHashSet<>();
        collectTemplateItems(nullToBlank(template.getRequiredItems()), items, seenPaths);
        collectTemplateItems(nullToBlank(template.getParsedSections()), items, seenPaths);
        if (items.isEmpty()) {
            return "1. 文章结构 - 请根据模板原文检查完整文章结构";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            StructureAuditItem item = items.get(index);
            builder.append(index + 1).append(". ").append(item.path());
            if (!item.requirement().isBlank()) {
                builder.append(" - ").append(item.requirement());
            }
            builder.append("\n");
        }
        return builder.toString().stripTrailing();
    }

    private static void collectTemplateItems(String json, List<StructureAuditItem> items, Set<String> seenPaths) {
        JsonNode root = LlmJsonPayloadExtractor.extract(new ObjectMapper(), json).orElse(null);
        if (root == null) {
            collectPlainTemplateItems(json, items, seenPaths);
            return;
        }
        if (root.isArray()) {
            for (JsonNode node : root) {
                collectTemplateNode(node, "", items, seenPaths);
            }
        } else if (root.isObject()) {
            collectTemplateNode(root, "", items, seenPaths);
        }
    }

    private static void collectPlainTemplateItems(String text, List<StructureAuditItem> items, Set<String> seenPaths) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.strip();
            if (line.isBlank()) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon >= 0) {
                String parent = cleanPlainItem(line.substring(0, colon));
                addStructureAuditItem(parent, "", items, seenPaths);
                String childText = line.substring(colon + 1);
                String[] children = childText.contains(";") ? childText.split(";") : childText.split(",");
                for (String child : children) {
                    addStructureAuditItem(parent + " > " + cleanPlainItem(child), "", items, seenPaths);
                }
            } else {
                for (String section : line.split(";")) {
                    addStructureAuditItem(cleanPlainItem(section), "", items, seenPaths);
                }
            }
        }
    }

    private static void addStructureAuditItem(String path, String requirement, List<StructureAuditItem> items, Set<String> seenPaths) {
        String cleanPath = cleanPlainItem(path);
        if (!cleanPath.isBlank() && seenPaths.add(cleanPath)) {
            items.add(new StructureAuditItem(cleanPath, requirement));
        }
    }

    private static String cleanPlainItem(String value) {
        if (value == null) {
            return "";
        }
        return value.strip().replaceAll("[。.;；]+$", "").strip();
    }

    private static void collectTemplateNode(JsonNode node, String parentPath, List<StructureAuditItem> items, Set<String> seenPaths) {
        if (!node.isObject()) {
            return;
        }
        String name = firstNonBlank(
                textValue(node, "name"),
                textValue(node, "title"),
                textValue(node, "item"),
                textValue(node, "requirement")
        );
        String currentPath = parentPath;
        if (!name.isBlank()) {
            currentPath = parentPath.isBlank() ? name : parentPath + " > " + name;
            if (seenPaths.add(currentPath)) {
                items.add(new StructureAuditItem(currentPath, textValue(node, "requirement")));
            }
        }
        collectChildren(node, "children", currentPath, items, seenPaths);
        collectChildren(node, "sub_items", currentPath, items, seenPaths);
        collectChildren(node, "subItems", currentPath, items, seenPaths);
        collectChildren(node, "items", currentPath, items, seenPaths);
    }

    private static void collectChildren(JsonNode node, String fieldName, String currentPath, List<StructureAuditItem> items, Set<String> seenPaths) {
        JsonNode children = node.get(fieldName);
        if (children == null || !children.isArray()) {
            return;
        }
        for (JsonNode child : children) {
            collectTemplateNode(child, currentPath, items, seenPaths);
        }
    }

    private static String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null ? "" : value.asText("").strip();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String auditChecklist(LlmProviderConfig provider, ExtractedDocument document, Optional<ChecklistTemplate> template) {
        if (template.isEmpty()) {
            return """
                    {"status":"missing_checklist","message":"未找到该综述类型对应的审核清单"}
                    """;
        }
        List<ChecklistItem> items = checklistItemRepository.findByChecklistTemplate_IdOrderByOrderIndexAsc(template.get().getId());
        String prompt = buildChecklistAuditPrompt(items, document);
        return llmClient.complete(provider, new LlmRequest("你是医学期刊综述稿件审核清单审查助手。", prompt)).content();
    }

    static String buildChecklistAuditPrompt(List<ChecklistItem> items, ExtractedDocument document) {
        StringBuilder checklist = new StringBuilder();
        for (ChecklistItem item : items) {
            checklist.append(item.getOrderIndex())
                    .append(". [")
                    .append(item.getCategory())
                    .append("] ")
                    .append(item.getRequirement())
                    .append("\n评价说明：")
                    .append(item.getEvaluationGuidance() == null ? "" : item.getEvaluationGuidance())
                    .append(extraChecklistGuidance(item))
                    .append("\n\n");
        }
        return """
                请根据审核清单逐项检查稿件是否撰写到位。每项必须输出 JSON 数组元素，字段包括 item, status, evidence, reason, suggestion, suggested_location。
                status 只能使用：已撰写、未撰写、撰写不完整、无法判断、不适用。
                只输出合法 JSON 数组，不要输出思考过程，不要输出 Markdown 代码块。
                必须用中文输出字段内容。
                字段值中不要使用英文双引号引用原文；如需引用原文，请改用中文单引号或概述原文。
                每一项都必须结合稿件原文给出具体分析，不能只给笼统结论。

                审核清单：
                %s

                稿件全文：
                %s
                """.formatted(checklist, document.fullText());
    }

    private static String extraChecklistGuidance(ChecklistItem item) {
        String category = item.getCategory() == null ? "" : item.getCategory();
        String requirement = item.getRequirement() == null ? "" : item.getRequirement();
        if (!category.contains("主要内容") && !requirement.contains("主要内容")) {
            return "";
        }
        return """

                主要内容专项审查要求：
                - 内容科学性：检查核心论点、数据解释、临床结论是否有充分文献或稿件数据支撑，是否存在夸大、过度推断或与证据不一致。
                - 逻辑性：检查主体各小节之间是否有清晰递进关系，比较维度是否一致，结论是否从前文证据自然推出。
                - 证据支撑：逐项核对关键结论是否引用了具体研究、系统综述、RCT、队列研究或表格数据，指出证据薄弱或引用不足的位置。
                - 综述分析深度：判断作者是否只是罗列文献，还是比较了研究差异、局限性、适用条件、异质性和临床意义。
                - 图表与正文呼应：检查表格/图示是否服务于主要论证，是否被正文解释和引用。
                - 必须引用稿件中的具体章节、段落或关键表述作为 evidence，并在 reason 中说明科学性或逻辑性问题。不能只给笼统结论。
                - reason 必须按【内容科学性】【逻辑性】【证据支撑】【综述分析深度】【图表与正文呼应】五个维度分别给出判断；某一维度没有明显问题时，也要说明稿件中已有的具体支撑点。
                """;
    }

    private Optional<String> extractReviewTypeName(String classificationJson) {
        try {
            JsonNode root = LlmJsonPayloadExtractor.extract(objectMapper, classificationJson).orElseThrow();
            JsonNode reviewType = root.get("review_type");
            if (reviewType != null && !reviewType.asText().isBlank()) {
                return Optional.of(reviewType.asText());
            }
        } catch (Exception ignored) {
            // The raw model response is still saved in the audit job for manual inspection.
        }
        return Optional.empty();
    }

    private String normalizeJsonPayload(String value) {
        return LlmJsonPayloadExtractor.normalize(objectMapper, value);
    }

    static String normalizeAuditPayload(ObjectMapper objectMapper, String value) {
        JsonNode root = LlmJsonPayloadExtractor.extractComplete(objectMapper, value).orElse(null);
        if (root == null) {
            return value;
        }
        if (root.isArray() || root.has("message")) {
            return root.toString();
        }
        return value;
    }

    private record StructureAuditItem(String path, String requirement) {
    }
}
