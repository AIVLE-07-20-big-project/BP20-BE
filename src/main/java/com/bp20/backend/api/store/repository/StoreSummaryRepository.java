package com.bp20.backend.api.store.repository;

import com.bp20.backend.api.store.domain.StoreSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSummaryRepository extends JpaRepository<StoreSummary, Long> {

    Optional<StoreSummary> findTopByStoreIdOrderByIdDesc(Long storeId);

}
