package com.bp20.backend.recommendation.dto;

public record OrderRecommendationResponse(
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
        String recommendationReason
) {
}