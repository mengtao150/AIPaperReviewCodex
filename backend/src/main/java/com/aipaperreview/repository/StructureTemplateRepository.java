package com.aipaperreview.repository;

import com.aipaperreview.domain.StructureTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StructureTemplateRepository extends JpaRepository<StructureTemplate, Long> {
    @EntityGraph(attributePaths = "reviewType")
    List<StructureTemplate> findByActiveTrueOrderByNameAsc();

    @EntityGraph(attributePaths = "reviewType")
    Optional<StructureTemplate> findFirstByReviewType_NameIgnoreCaseAndActiveTrueOrderByVersionDesc(String reviewTypeName);
}
