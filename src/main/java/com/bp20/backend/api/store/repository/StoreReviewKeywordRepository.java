package com.bp20.backend.api.store.repository;

import com.bp20.backend.api.store.domain.StoreReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreReviewKeywordRepository extends JpaRepository<StoreReviewKeyword, Long> {

    List<StoreReviewKeyword> findAllByStoreId(Long storeId);

}
