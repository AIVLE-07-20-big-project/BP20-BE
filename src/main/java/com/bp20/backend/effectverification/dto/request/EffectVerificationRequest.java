package com.bp20.backend.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EffectVerificationRequest {

    @JsonProperty("store_id")
    private Long storeId;

    @JsonProperty("recommendation_id")
    private Long recommendationId;

    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    private VerificationCondition condition;

    private PeriodMetrics before;

    private PeriodMetrics after;
}