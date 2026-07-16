package com.bp20.backend.forecast;

import java.time.LocalDate;
import java.util.List;

public record ForecastRequest(
        int forecastDays,
        List<ProductSalesHistory> products
) {
    public record ProductSalesHistory(
            String productCode,
            String productName,
            List<DailySalesValue> salesHistory
    ) {
    }

    public record DailySalesValue(
            LocalDate saleDate,
            long salesQuantity,
            long unitPrice
    ) {}
}