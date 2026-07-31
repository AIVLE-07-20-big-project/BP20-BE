package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByStoreId(Long storeId);

    List<Review> findTop30ByStoreIdAndIsAnalyzedFalse(Long storeId);

    @Query("SELECT DISTINCT r.storeId FROM Review r WHERE r.isAnalyzed = false")
    List<Long> findStoreIdWithUnanalyzedReviews();

    List<Review> findAllByStoreIdAndIsAnalyzedFalse(Long storeId);
}
