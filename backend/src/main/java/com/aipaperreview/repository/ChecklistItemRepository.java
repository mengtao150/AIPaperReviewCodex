package com.aipaperreview.repository;

import com.aipaperreview.domain.ChecklistItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByChecklistTemplate_IdOrderByOrderIndexAsc(Long checklistTemplateId);
}
