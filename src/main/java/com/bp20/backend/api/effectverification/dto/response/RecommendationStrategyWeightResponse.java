package com.bp20.backend.api.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record RecommendationStrategyWeightResponse(
        @JsonProperty("store_id") Long storeId,
        @JsonProperty("action_id") String actionId,
        Double weight,
        @JsonProperty("last_effect_score") Double lastEffectScore,
        @JsonProperty("last_verdict") String lastVerdict,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
}
