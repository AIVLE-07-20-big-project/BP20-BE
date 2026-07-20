package com.bp20.backend.api.budget.repository;

import com.bp20.backend.api.budget.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByStoreId(Long storeId);

    Optional<Budget> findByStoreIdAndYearMonthAndCategory(Long storeId, String yearMonth, String category);
}
