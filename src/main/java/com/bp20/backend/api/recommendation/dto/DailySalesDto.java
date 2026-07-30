package com.bp20.backend.api.recommendation.dto;

import java.time.LocalDate;

public record DailySalesDto(
        LocalDate saleDate,
        String productCode,
        String productName,
        long salesQuantity,
        long unitPrice,
        long salesAmount
) {
}
