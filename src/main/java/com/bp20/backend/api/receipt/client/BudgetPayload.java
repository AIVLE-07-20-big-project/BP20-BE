package com.bp20.backend.api.receipt.client;

import com.bp20.backend.api.budget.domain.Budget;

public record BudgetPayload(
        String yearMonth,
        String category,
        Integer budgetAmount,
        Long storeId
) {
    public static BudgetPayload from(Budget budget) {
        return new BudgetPayload(
                budget.getYearMonth(), budget.getCategory(), budget.getBudgetAmount(), budget.getStoreId()
        );
    }
}
