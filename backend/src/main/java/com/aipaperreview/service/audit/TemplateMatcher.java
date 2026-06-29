package com.aipaperreview.service.audit;

import com.aipaperreview.domain.StructureTemplate;
import com.aipaperreview.repository.StructureTemplateRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TemplateMatcher {
    private final StructureTemplateRepository repository;

    public TemplateMatcher(StructureTemplateRepository repository) {
        this.repository = repository;
    }

    public Optional<StructureTemplate> match(String reviewTypeName) {
        return repository.findFirstByReviewType_NameIgnoreCaseAndActiveTrueOrderByVersionDesc(reviewTypeName);
    }
}
