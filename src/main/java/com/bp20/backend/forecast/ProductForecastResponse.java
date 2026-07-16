package com.bp20.backend.forecast;

import java.util.List;

public record ProductForecastResponse(
        String selectedModel,
        List<ProductForecast> forecasts
) {
    public record ProductForecast(
            String productCode,

            // 향후 7일 전체 예상 판매량
            long predictedSalesQuantity,

            // 예측 하한
            long lowerBound,

            // 예측 상한
            long upperBound,

            // 모델 예측 신뢰도
            double confidenceScore
    ) {
    }
}