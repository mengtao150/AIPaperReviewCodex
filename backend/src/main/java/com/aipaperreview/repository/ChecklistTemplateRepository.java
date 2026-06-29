package com.aipaperreview.repository;

import com.aipaperreview.domain.ChecklistTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {
    @EntityGraph(attributePaths = "reviewType")
    List<ChecklistTemplate> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "reviewType")
    Optional<ChecklistTemplate> findFirstByReviewType_NameIgnoreCaseAndActiveTrueOrderByVersionDesc(String reviewTypeName);
}
