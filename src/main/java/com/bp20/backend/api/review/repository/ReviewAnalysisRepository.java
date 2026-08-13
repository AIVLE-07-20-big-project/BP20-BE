package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.ReviewAnalysis;
import com.bp20.backend.api.review.dto.AspectScoreDto;
import com.bp20.backend.api.review.dto.response.AspectStatResponseDto;
import com.bp20.backend.api.review.dto.response.ReviewTrendResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReviewAnalysisRepository extends JpaRepository<ReviewAnalysis, Long> {

    List<ReviewAnalysis> findByReview_Store_IdAndReview_ReviewedDateGreaterThanEqualAndReview_ReviewedDateLessThan(
            Long storeId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
    
    @Query("SELECT new com.bp20.backend.api.review.dto.AspectScoreDto(" +
           "  ra.aspect, " +
           "  ROUND(AVG(CASE WHEN ra.sentiment = '긍정' THEN 5.0 " +
           "                 WHEN ra.sentiment = '중립' THEN 3.0 " +
           "                 WHEN ra.sentiment = '부정' THEN 1.0 ELSE 0.0 END), 1) " +
           ") " +
           "FROM ReviewAnalysis ra " +
           "WHERE ra.review.store.id = :storeId " +
           "  AND ra.review.reviewedDate >= :startDate " +
           "  AND ra.review.reviewedDate < :endDate " +
           "GROUP BY ra.aspect")
    List<AspectScoreDto> findAspectScoresByStoreAndDate(
        @Param("storeId") Long storeId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT new com.bp20.backend.api.review.dto.response.AspectStatResponseDto(" +
            "  ra.aspect, " +
            "  SUM(CASE WHEN ra.sentiment = '긍정' THEN 1L ELSE 0L END), " +
            "  SUM(CASE WHEN ra.sentiment = '중립' THEN 1L ELSE 0L END), " +
            "  SUM(CASE WHEN ra.sentiment = '부정' THEN 1L ELSE 0L END) " +
            ") " +
            "FROM ReviewAnalysis ra " +
            "WHERE ra.review.store.id = :storeId " +
            "GROUP BY ra.aspect")
    List<AspectStatResponseDto> findAspectStatsByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT new com.bp20.backend.api.review.dto.response.ReviewTrendResponseDto(" +
            "  CAST(FUNCTION('DATE_FORMAT', r.reviewedDate, '%x-%v') AS string), " +
            "  ROUND(AVG(r.rating), 1), " +
            "  COUNT(DISTINCT ra.review.id) " +
            ") " +
            "FROM Review r " +
            "LEFT JOIN ReviewAnalysis ra " +
            "  ON ra.review = r AND ra.sentiment = '부정' " +
            "WHERE r.store.id = :storeId " +
            "  AND r.reviewedDate >= :startDate " +
            "  AND r.reviewedDate < :endDate " +
            "GROUP BY CAST(FUNCTION('DATE_FORMAT', r.reviewedDate, '%x-%v') AS string) " +
            "ORDER BY CAST(FUNCTION('DATE_FORMAT', r.reviewedDate, '%x-%v') AS string)")
    List<ReviewTrendResponseDto> findWeeklyTrendByStoreId(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
