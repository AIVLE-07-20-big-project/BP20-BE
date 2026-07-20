package com.bp20.backend.api.receipt.repository;

import com.bp20.backend.api.receipt.domain.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    Optional<Receipt> findByDedupeKey(String dedupeKey);

    boolean existsByDedupeKey(String dedupeKey);

    List<Receipt> findByStoreIdAndTransactionDateBetweenOrderByTransactionDateAsc(
            Long storeId, LocalDate from, LocalDate to);

    List<Receipt> findByStoreIdOrderByTransactionDateDesc(Long storeId);
}
