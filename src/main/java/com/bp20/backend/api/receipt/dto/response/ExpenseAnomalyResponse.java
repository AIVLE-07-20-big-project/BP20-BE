package com.bp20.backend.api.receipt.dto.response;

public record ExpenseAnomalyResponse(
        String category,
        String week,
        Integer weeklyAmount,
        Integer categoryAvg,
        Double zScore,
        String direction
) {
}
