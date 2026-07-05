package com.aipaperreview.repository;

import com.aipaperreview.domain.AuditJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditJobRepository extends JpaRepository<AuditJob, Long> {
    List<AuditJob> findAllByOrderByCreatedAtDesc();
}
