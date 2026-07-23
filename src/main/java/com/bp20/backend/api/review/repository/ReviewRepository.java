package com.bp20.backend.api.review.repository;

import com.bp20.backend.api.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByIsAnalyzedFalse();

    List<Review> findTop50ByIsAnalyzedFalse();

//    Page<Review> findByStoreId(Long storeId, Pageable pageable);

//    long countByStoreIdAndIsAnalyzedFalse(Long storeId);
}
