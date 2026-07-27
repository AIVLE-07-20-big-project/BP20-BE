package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.ReviewAnalysis;
import com.bp20.backend.api.review.dto.AspectScoreDto;
import com.bp20.backend.api.review.dto.response.AspectStatResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {

    @Query("SELECT new com.bp20.backend.api.review.dto.response.AspectStatResponseDto(" +
            "  ra.aspect, " +
            "  SUM(CASE WHEN ra.sentiment = '긍정' THEN 1L ELSE 0L END), " +
            "  SUM(CASE WHEN ra.sentiment = '중립' THEN 1L ELSE 0L END), " +
            "  SUM(CASE WHEN ra.sentiment = '부정' THEN 1L ELSE 0L END) " +
            ") " +
            "FROM ReviewAnalysis ra " +
            "JOIN Review r ON ra.reviewId = r.id " +
            "WHERE r.storeId = :storeId " +
            "GROUP BY ra.aspect")
    List<AspectStatResponseDto> findAspectStatsByStoreId(@Param("storeId") Long storeId);
}
