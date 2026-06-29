package com.aipaperreview.service.audit;

import com.aipaperreview.domain.ChecklistTemplate;
import com.aipaperreview.repository.ChecklistTemplateRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ChecklistMatcher {
    private final ChecklistTemplateRepository repository;

    public ChecklistMatcher(ChecklistTemplateRepository repository) {
        this.repository = repository;
    }

    public Optional<ChecklistTemplate> match(String reviewTypeName) {
        return repository.findFirstByReviewType_NameIgnoreCaseAndActiveTrueOrderByVersionDesc(reviewTypeName);
    }
}
