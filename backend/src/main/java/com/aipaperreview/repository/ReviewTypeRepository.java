package com.aipaperreview.repository;

import com.aipaperreview.domain.ReviewType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTypeRepository extends JpaRepository<ReviewType, Long> {
    List<ReviewType> findByActiveTrueOrderByNameAsc();

    Optional<ReviewType> findByNameIgnoreCaseAndActiveTrue(String name);
}
