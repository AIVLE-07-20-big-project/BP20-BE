package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.ReviewAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {
}
