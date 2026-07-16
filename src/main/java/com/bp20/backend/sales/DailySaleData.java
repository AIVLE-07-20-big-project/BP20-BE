package com.bp20.backend.sales;

import java.time.LocalDate;

public record DailySaleData(
        LocalDate saleDate,
        String productCode,
        String productName,
        long salesQuantity,
        long unitPrice,
        long salesAmount
) {
}