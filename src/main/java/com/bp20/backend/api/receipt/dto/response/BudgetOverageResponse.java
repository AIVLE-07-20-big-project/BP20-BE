package com.bp20.backend.api.receipt.dto.response;

public record BudgetOverageResponse(
        String yearMonth,
        String category,
        Integer actualAmount,
        Integer budgetAmount,
        Integer overAmount,
        Double overPct   // 예산 자체가 없는 경우 null (nan% 같은 깨진 값 대신 null로 내려옴)
) {
}
