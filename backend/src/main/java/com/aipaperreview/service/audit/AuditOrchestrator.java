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
import java.util.List;
import java.util.Optional;
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
            job.setStatus(AuditJob.Status.EXTRACTED);
            auditJobRepository.save(job);

            LlmProviderConfig provider = llmProviderService.activeProvider();
            String classification = classify(provider, document);
            job.setClassificationResult(classification);
            String reviewTypeName = extractReviewTypeName(classification).orElse("Narrative Review");
            ReviewType reviewType = reviewTypeRepository.findByNameIgnoreCaseAndActiveTrue(reviewTypeName)
                    .orElseGet(() -> reviewTypeRepository.findByNameIgnoreCaseAndActiveTrue("Narrative Review").orElse(null));
            job.setDetectedReviewType(reviewType);
            job.setStatus(AuditJob.Status.CLASSIFIED);
            auditJobRepository.save(job);

            Optional<StructureTemplate> structureTemplate = templateMatcher.match(reviewTypeName);
            String structureAudit = auditStructure(provider, document, structureTemplate);
            job.setStructureAuditResult(structureAudit);
            job.setStatus(AuditJob.Status.STRUCTURE_AUDITED);
            auditJobRepository.save(job);

            Optional<ChecklistTemplate> checklistTemplate = checklistMatcher.match(reviewTypeName);
            String checklistAudit = auditChecklist(provider, document, checklistTemplate);
            job.setChecklistAuditResult(checklistAudit);
            job.setStatus(AuditJob.Status.CHECKLIST_AUDITED);

            String report = reportBuilder.buildMarkdown(
                    originalFilename,
                    classification,
                    structureTemplate,
                    structureAudit,
                    checklistTemplate,
                    checklistAudit
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
        return """
                请根据文章结构模板检查稿件是否按模板撰写。
                审查时必须同时检查：
                1. 一级标题是否存在、顺序是否合理；
                2. 每一个标题下面的小标题、子标题或子项是否存在；
                3. 每个标题或小标题对应的内容是否写到位。

                请把 parsed_sections 中的 children/sub_items 也作为独立结构审查项，不要只检查一级标题。
                逐项输出 JSON 数组，每项包含 item, hierarchy_path, status, evidence, reason, suggestion。
                status 只能使用：已撰写、未撰写、撰写不完整、无法判断、不适用。

                文章结构模板 parsed_sections（包含标题及小标题层级）：
                %s

                文章结构模板 required_items（必需结构项）：
                %s

                稿件全文：
                %s
                """.formatted(
                nullToBlank(template.getParsedSections()),
                nullToBlank(template.getRequiredItems()),
                document.fullText()
        );
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
        StringBuilder checklist = new StringBuilder();
        for (ChecklistItem item : items) {
            checklist.append(item.getOrderIndex())
                    .append(". [")
                    .append(item.getCategory())
                    .append("] ")
                    .append(item.getRequirement())
                    .append("\n评价说明：")
                    .append(item.getEvaluationGuidance() == null ? "" : item.getEvaluationGuidance())
                    .append("\n\n");
        }
        String prompt = """
                请根据审核清单逐项检查稿件是否撰写到位。每项必须输出 JSON 数组元素，字段包括 item, status, evidence, reason, suggestion, suggested_location。
                status 只能使用：已撰写、未撰写、撰写不完整、无法判断、不适用。

                审核清单：
                %s

                稿件全文：
                %s
                """.formatted(checklist, document.fullText());
        return llmClient.complete(provider, new LlmRequest("你是医学期刊综述稿件审核清单审查助手。", prompt)).content();
    }

    private Optional<String> extractReviewTypeName(String classificationJson) {
        try {
            JsonNode root = objectMapper.readTree(classificationJson);
            JsonNode reviewType = root.get("review_type");
            if (reviewType != null && !reviewType.asText().isBlank()) {
                return Optional.of(reviewType.asText());
            }
        } catch (Exception ignored) {
            // The raw model response is still saved in the audit job for manual inspection.
        }
        return Optional.empty();
    }
}
