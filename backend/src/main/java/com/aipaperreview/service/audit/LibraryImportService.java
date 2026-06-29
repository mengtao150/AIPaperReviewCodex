package com.aipaperreview.service.audit;

import com.aipaperreview.domain.ChecklistItem;
import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.domain.LlmProviderConfig;
import com.aipaperreview.domain.ReviewType;
import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.repository.ChecklistTemplateRepository;
import com.aipaperreview.repository.ReviewTypeRepository;
import com.aipaperreview.repository.StructureTemplateRepository;
import com.aipaperreview.service.document.SourceTextExtractor;
import com.aipaperreview.service.llm.LlmClient;
import com.aipaperreview.service.llm.LlmProviderService;
import com.aipaperreview.service.llm.LlmRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LibraryImportService {
    private final ReviewTypeRepository reviewTypeRepository;
    private final StructureTemplateRepository structureTemplateRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final SourceTextExtractor sourceTextExtractor;
    private final LlmProviderService llmProviderService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LibraryImportService(
            ReviewTypeRepository reviewTypeRepository,
            StructureTemplateRepository structureTemplateRepository,
            ChecklistTemplateRepository checklistTemplateRepository,
            SourceTextExtractor sourceTextExtractor,
            LlmProviderService llmProviderService,
            LlmClient llmClient,
            ObjectMapper objectMapper
    ) {
        this.reviewTypeRepository = reviewTypeRepository;
        this.structureTemplateRepository = structureTemplateRepository;
        this.checklistTemplateRepository = checklistTemplateRepository;
        this.sourceTextExtractor = sourceTextExtractor;
        this.llmProviderService = llmProviderService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public StructureTemplate importStructureTemplate(Long reviewTypeId, String name, String version, MultipartFile file)
            throws IOException {
        ReviewType reviewType = reviewTypeRepository.findById(reviewTypeId).orElseThrow();
        String sourceText = sourceTextExtractor.extract(file.getOriginalFilename(), file.getInputStream());
        String parsedJson = parseStructureWithLlm(reviewType.getName(), sourceText);
        JsonNode root = objectMapper.readTree(parsedJson);

        StructureTemplate template = new StructureTemplate();
        template.setReviewType(reviewType);
        template.setName(blankDefault(name, file.getOriginalFilename()));
        template.setVersion(blankDefault(version, "1.0"));
        template.setSourceFilePath(file.getOriginalFilename());
        template.setParsedSections(root.path("parsed_sections").toString());
        template.setRequiredItems(root.path("required_items").toString());
        template.setOptionalItems(root.path("optional_items").toString());
        template.setActive(true);
        return structureTemplateRepository.save(template);
    }

    public ChecklistTemplate importChecklistTemplate(Long reviewTypeId, String name, String version, MultipartFile file)
            throws IOException {
        ReviewType reviewType = reviewTypeRepository.findById(reviewTypeId).orElseThrow();
        String sourceText = sourceTextExtractor.extract(file.getOriginalFilename(), file.getInputStream());
        String parsedJson = parseChecklistWithLlm(reviewType.getName(), sourceText);
        JsonNode root = objectMapper.readTree(parsedJson);

        ChecklistTemplate template = new ChecklistTemplate();
        template.setReviewType(reviewType);
        template.setName(blankDefault(name, file.getOriginalFilename()));
        template.setVersion(blankDefault(version, "1.0"));
        template.setSourceFilePath(file.getOriginalFilename());
        template.setNotes(root.path("notes").asText(""));
        template.setActive(true);

        JsonNode items = root.path("items");
        if (items.isArray()) {
            int index = 1;
            for (JsonNode node : items) {
                ChecklistItem item = new ChecklistItem();
                item.setChecklistTemplate(template);
                item.setCategory(node.path("category").asText("未分类"));
                item.setRequirement(node.path("requirement").asText());
                item.setRequiredItem(node.path("required").asBoolean(true));
                item.setEvaluationGuidance(node.path("evaluation_guidance").asText(""));
                item.setOrderIndex(node.path("order_index").asInt(index));
                template.getItems().add(item);
                index++;
            }
        }
        return checklistTemplateRepository.save(template);
    }

    private String parseStructureWithLlm(String reviewType, String sourceText) {
        LlmProviderConfig provider = llmProviderService.activeProvider();
        String prompt = buildStructureParsePrompt(reviewType, sourceText);
        return llmClient.complete(provider, new LlmRequest("你是学术期刊文章结构模板解析助手。", prompt)).content();
    }

    static String buildStructureParsePrompt(String reviewType, String sourceText) {
        return """
                请从下面的 Word/PDF 模板文本中解析出“文章结构模板规则”。综述类型：%s。
                只输出合法 JSON，不要输出 Markdown。

                关键要求：
                1. 不只抽取一级标题。每一个标题下面的小标题、子标题、子项说明也都必须作为后续结构审查项。
                2. 请保留标题层级关系。一级标题放在 parsed_sections 中，小标题放在 children 或 sub_items 中。
                3. 如果某个标题或小标题下面有具体内容要求，也要写入 requirement，不能只保留标题名称。
                4. 审查项应覆盖作者写作时必须出现的结构内容，例如 Introduction 下的 Background、Rationale and knowledge gap、Objective。

                JSON schema:
                {
                  "parsed_sections": [
                    {
                      "name": "一级标题或结构项",
                      "requirement": "该标题整体要求",
                      "order_index": 1,
                      "children": [
                        {
                          "name": "小标题或子项",
                          "requirement": "小标题下需要撰写的内容",
                          "order_index": 1,
                          "sub_items": [
                            {
                              "name": "更细分的子项",
                              "requirement": "更细分子项的写作要求",
                              "order_index": 1
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "required_items": [
                    {
                      "name": "必需标题或子标题",
                      "level": 1,
                      "parent": null,
                      "requirement": "具体要求",
                      "order_index": 1,
                      "children": []
                    }
                  ],
                  "optional_items": [
                    {
                      "name": "可选标题或子标题",
                      "level": 1,
                      "parent": null,
                      "requirement": "具体要求",
                      "order_index": 1,
                      "children": []
                    }
                  ],
                  "notes": "补充说明"
                }

                模板文本：
                %s
                """.formatted(reviewType, sourceText);
    }

    private String parseChecklistWithLlm(String reviewType, String sourceText) {
        LlmProviderConfig provider = llmProviderService.activeProvider();
        String prompt = """
                请从下面的 Word/PDF 清单文本中解析出“审核清单规则”。只抽取与综述类型 %s 对应的具体要求。
                只输出合法 JSON，不要输出 Markdown。
                JSON schema:
                {
                  "notes": "来源和适用范围说明",
                  "items": [
                    {
                      "category": "题目/摘要/关键词/引言/方法/主要内容/结论/其他",
                      "requirement": "具体审核要求",
                      "required": true,
                      "evaluation_guidance": "大模型审查时应如何判断该项是否撰写",
                      "order_index": 1
                    }
                  ]
                }

                清单文本：
                %s
                """.formatted(reviewType, sourceText);
        return llmClient.complete(provider, new LlmRequest("你是医学期刊综述审核清单解析助手。", prompt)).content();
    }

    private String blankDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
