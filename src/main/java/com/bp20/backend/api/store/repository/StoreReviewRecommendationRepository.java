package com.bp20.backend.api.store.repository;

import com.bp20.backend.api.store.domain.StoreReviewRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreReviewRecommendationRepository extends JpaRepository<StoreReviewRecommendation, Long> {

    Optional<StoreReviewRecommendation> findTopByStore_IdOrderByCreatedAtDesc(Long storeId);

    boolean existsByStore_IdAndReportMonth(Long storeId, String reportMonth);

}
