package com.aipaperreview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "checklist_item")
public class ChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_template_id", nullable = false)
    private ChecklistTemplate checklistTemplate;

    @Column(nullable = false, length = 120)
    private String category;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String requirement;

    @Column(nullable = false)
    private boolean requiredItem = true;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String evaluationGuidance;

    @Column(nullable = false)
    private int orderIndex;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ChecklistTemplate getChecklistTemplate() {
        return checklistTemplate;
    }

    public void setChecklistTemplate(ChecklistTemplate checklistTemplate) {
        this.checklistTemplate = checklistTemplate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public boolean isRequiredItem() {
        return requiredItem;
    }

    public void setRequiredItem(boolean requiredItem) {
        this.requiredItem = requiredItem;
    }

    public String getEvaluationGuidance() {
        return evaluationGuidance;
    }

    public void setEvaluationGuidance(String evaluationGuidance) {
        this.evaluationGuidance = evaluationGuidance;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
