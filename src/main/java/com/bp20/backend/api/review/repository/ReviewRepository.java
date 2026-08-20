package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStore_Id(Long storeId);

    List<Review> findTop30ByStore_IdAndIsAnalyzedFalseOrderByReviewedDateAscIdAsc(Long storeId);

    @Query("""
            SELECT r.store.id
            FROM Review r
            WHERE r.isAnalyzed = false
            GROUP BY r.store.id
            HAVING COUNT(r) >= :minimumReviewCount
            """)
    List<Long> findStoreIdsWithAtLeastUnanalyzedReviews(
            @Param("minimumReviewCount") long minimumReviewCount
    );
}
