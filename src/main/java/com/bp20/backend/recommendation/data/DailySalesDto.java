package com.bp20.backend.recommendation.data;

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
