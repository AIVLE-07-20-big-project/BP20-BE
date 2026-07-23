package com.bp20.backend.recommendation.dto;

import com.bp20.backend.recommendation.entity.OrderRecommendationHistory;

import java.time.LocalDateTime;

public record OrderRecommendationHistoryResponse(
        Long id,
        String requestId,
        String ingredientName,

        long currentStock,
        long reservedStock,
        long availableStock,
        long incomingStock,
        long safetyStock,

        long expectedUsage,
        long recommendedOrderQuantity,
        boolean orderRequired,

        double confidenceScore,
        String modelName,
        String recommendationReason,

        double latitude,
        double longitude,

        Double temperature,
        Double windSpeed,
        String sky,
        String precipitationType,
        Integer rainProbability,
        Integer humidity,

        LocalDateTime orderDateTime,
        LocalDateTime forecastDateTime,
        LocalDateTime createdAt
) {

    public static OrderRecommendationHistoryResponse from(
            OrderRecommendationHistory history
    ) {
        return new OrderRecommendationHistoryResponse(
                history.getId(),
                history.getRequestId(),
                history.getIngredientName(),

                history.getCurrentStock(),
                history.getReservedStock(),
                history.getAvailableStock(),
                history.getIncomingStock(),
                history.getSafetyStock(),

                history.getExpectedUsage(),
                history.getRecommendedOrderQuantity(),
                history.isOrderRequired(),

                history.getConfidenceScore(),
                history.getModelName(),
                history.getRecommendationReason(),

                history.getLatitude(),
                history.getLongitude(),

                history.getTemperature(),
                history.getWindSpeed(),
                history.getSky(),
                history.getPrecipitationType(),
                history.getRainProbability(),
                history.getHumidity(),

                history.getOrderDateTime(),
                history.getForecastDateTime(),
                history.getCreatedAt()
        );
    }
}