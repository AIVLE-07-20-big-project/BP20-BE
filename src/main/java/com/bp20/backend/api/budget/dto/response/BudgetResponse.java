package com.bp20.backend.api.budget.dto.response;

import com.bp20.backend.api.budget.domain.Budget;

public record BudgetResponse(
        Long budgetId,
        Long storeId,
        String yearMonth,
        String category,
        Integer budgetAmount
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(), budget.getStoreId(), budget.getYearMonth(),
                budget.getCategory(), budget.getBudgetAmount()
        );
    }
}