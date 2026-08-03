package com.bp20.backend.api.effectverification.dto.response;

import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record EffectVerificationRoiResponse(
        @JsonProperty("total_verified") long totalVerified,
        @JsonProperty("average_effect_score") double averageEffectScore,
        @JsonProperty("effective_count") long effectiveCount,
        @JsonProperty("inconclusive_count") long inconclusiveCount,
        @JsonProperty("ineffective_count") long ineffectiveCount,
        @JsonProperty("store_summaries") List<StoreSummary> storeSummaries,
        @JsonProperty("type_summaries") List<TypeSummary> typeSummaries,
        @JsonProperty("recent_results") List<RecentResult> recentResults
) {
    public record StoreSummary(
            @JsonProperty("store_id") Long storeId,
            @JsonProperty("verified_count") long verifiedCount,
            @JsonProperty("average_effect_score") double averageEffectScore
    ) {
    }

    public record TypeSummary(
            @JsonProperty("recommendation_type") RecommendationType recommendationType,
            @JsonProperty("verified_count") long verifiedCount,
            @JsonProperty("average_effect_score") double averageEffectScore
    ) {
    }

    public record RecentResult(
            @JsonProperty("recommendation_id") String recommendationId,
            @JsonProperty("store_id") Long storeId,
            @JsonProperty("recommendation_type") RecommendationType recommendationType,
            @JsonProperty("effect_score") double effectScore,
            String verdict,
            @JsonProperty("verified_date") LocalDateTime verifiedDate
    ) {
    }
}
