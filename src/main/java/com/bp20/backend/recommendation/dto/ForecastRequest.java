package com.bp20.backend.forecast;

import java.util.List;

public record ForecastRequest(
        int forecastDays,
        String orderDateTime,
        WeatherFeature weather,
        List<ProductSalesHistory> products
) {

    public record WeatherFeature(
            String forecastDateTime,
            Double temperature,
            Double windSpeed,
            String sky,
            String precipitationType,
            Integer rainProbability,
            Integer humidity
    ) {
    }

    public record ProductSalesHistory(
            String productCode,
            String productName,
            List<DailySalesValue> salesHistory
    ) {
    }

    public record DailySalesValue(
            String saleDate,
            long salesQuantity,
            long unitPrice
    ) {}
}