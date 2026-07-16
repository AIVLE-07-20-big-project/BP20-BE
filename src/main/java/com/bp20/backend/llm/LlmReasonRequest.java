package com.bp20.backend.llm;

public record LlmReasonRequest(
        String ingredientName,
        long currentStock,
        long incomingStock,
        long safetyStock,
        long expectedUsage,
        long recommendedOrderQuantity,
        double confidenceScore,
        String modelName
) {
}