package com.aipaperreview.repository;

import com.aipaperreview.domain.AuditJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditJobRepository extends JpaRepository<AuditJob, Long> {
}
