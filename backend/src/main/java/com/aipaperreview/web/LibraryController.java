package com.aipaperreview.web;

import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.repository.ChecklistItemRepository;
import com.aipaperreview.repository.ChecklistTemplateRepository;
import com.aipaperreview.repository.ReviewTypeRepository;
import com.aipaperreview.repository.StructureTemplateRepository;
import com.aipaperreview.service.audit.LibraryImportService;
import com.aipaperreview.web.dto.ChecklistDetailResponse;
import com.aipaperreview.web.dto.ImportTemplateResponse;
import com.aipaperreview.web.dto.LibraryItemResponse;
import com.aipaperreview.web.dto.StructureTemplateDetailResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@CrossOrigin
@RestController
@RequestMapping("/api/library")
public class LibraryController {
    private final ReviewTypeRepository reviewTypeRepository;
    private final StructureTemplateRepository structureTemplateRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final LibraryImportService libraryImportService;

    public LibraryController(
            ReviewTypeRepository reviewTypeRepository,
            StructureTemplateRepository structureTemplateRepository,
            ChecklistTemplateRepository checklistTemplateRepository,
            ChecklistItemRepository checklistItemRepository,
            LibraryImportService libraryImportService
    ) {
        this.reviewTypeRepository = reviewTypeRepository;
        this.structureTemplateRepository = structureTemplateRepository;
        this.checklistTemplateRepository = checklistTemplateRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.libraryImportService = libraryImportService;
    }

    @GetMapping("/review-types")
    public List<LibraryItemResponse> reviewTypes() {
        return reviewTypeRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(item -> new LibraryItemResponse(item.getId(), item.getName(), item.getName(), "", item.isActive()))
                .toList();
    }

    @GetMapping("/structure-templates")
    public List<LibraryItemResponse> structureTemplates() {
        return structureTemplateRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(item -> new LibraryItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getReviewType().getName(),
                        item.getVersion(),
                        item.isActive()
                ))
                .toList();
    }

    @GetMapping("/checklists")
    public List<LibraryItemResponse> checklists() {
        return checklistTemplateRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(item -> new LibraryItemResponse(
                        item.getId(),
                        item.getName(),
                        item.getReviewType().getName(),
                        item.getVersion(),
                        item.isActive()
                ))
                .toList();
    }

    @GetMapping("/structure-templates/{id}")
    @Transactional(readOnly = true)
    public StructureTemplateDetailResponse structureTemplate(@PathVariable Long id) {
        StructureTemplate item = structureTemplateRepository.findById(id).orElseThrow();
        return new StructureTemplateDetailResponse(
                item.getId(),
                item.getName(),
                item.getReviewType().getName(),
                item.getVersion(),
                item.getParsedSections(),
                item.getRequiredItems(),
                item.getOptionalItems()
        );
    }

    @GetMapping("/checklists/{id}")
    @Transactional(readOnly = true)
    public ChecklistDetailResponse checklist(@PathVariable Long id) {
        ChecklistTemplate item = checklistTemplateRepository.findById(id).orElseThrow();
        List<ChecklistDetailResponse.Item> items = checklistItemRepository.findByChecklistTemplate_IdOrderByOrderIndexAsc(id)
                .stream()
                .map(check -> new ChecklistDetailResponse.Item(
                        check.getId(),
                        check.getCategory(),
                        check.getRequirement(),
                        check.isRequiredItem(),
                        check.getEvaluationGuidance(),
                        check.getOrderIndex()
                ))
                .toList();
        return new ChecklistDetailResponse(
                item.getId(),
                item.getName(),
                item.getReviewType().getName(),
                item.getVersion(),
                items
        );
    }

    @PostMapping("/structure-templates/import")
    public ImportTemplateResponse importStructureTemplate(
            @RequestParam Long reviewTypeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String version,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        StructureTemplate template = libraryImportService.importStructureTemplate(reviewTypeId, name, version, file);
        return new ImportTemplateResponse(
                template.getId(),
                template.getName(),
                template.getReviewType().getName(),
                template.getVersion(),
                template.getRequiredItems()
        );
    }

    @PostMapping("/checklists/import")
    public ImportTemplateResponse importChecklist(
            @RequestParam Long reviewTypeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String version,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        ChecklistTemplate template = libraryImportService.importChecklistTemplate(reviewTypeId, name, version, file);
        return new ImportTemplateResponse(
                template.getId(),
                template.getName(),
                template.getReviewType().getName(),
                template.getVersion(),
                template.getNotes()
        );
    }
}
