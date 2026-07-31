package com.bp20.backend.api.store.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record StoreRecommendationResponseDto(
        Long recommendationId,
        Long storeId,
        String executiveSummary,
        List<StoreRecommendationResponseDto.ActionItemDto> actionItems,
        LocalDateTime createdAt
) {
    public record ActionItemDto(
            String priority,
            String aspect,
            String keyword,
            String trendSummary,
            String problemCause,
            String actionPlan,
            String expectedOutcome,
            LocalDateTime executedAt
    ) {}
}
