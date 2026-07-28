package com.bp20.backend.api.effectverification.dto.response;

import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class EffectVerificationResponse {

    @JsonProperty("store_id")
    private Long storeId;

    @JsonProperty("recommendation_id")
    private Long recommendationId;

    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    @JsonProperty("effect_score")
    private Double effectScore;

    private String verdict;

    @JsonProperty("metric_results")
    private List<MetricResult> metricResults;

    private String summary;

    @JsonProperty("verified_date")
    private LocalDateTime verifiedDate;
}
