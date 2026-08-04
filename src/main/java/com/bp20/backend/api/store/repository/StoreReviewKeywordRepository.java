package com.bp20.backend.api.store.repository;

import com.bp20.backend.api.store.domain.StoreReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StoreReviewKeywordRepository extends JpaRepository<StoreReviewKeyword, Long> {

    List<StoreReviewKeyword> findByStore_IdOrderByCountDesc(Long storeId);

    List<StoreReviewKeyword> findByStore_IdAndAnalyzedAtGreaterThanEqualOrderByCountDesc(
            Long storeId, LocalDateTime startDate
    );

    List<StoreReviewKeyword> findByStore_IdAndAnalyzedAtBetween(
            Long storeId, LocalDateTime startDate, LocalDateTime endDate
    );
}
