package com.bp20.backend.api.reviewAnalysis.repository;

import com.bp20.backend.api.reviewAnalysis.domain.ReviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {
}
