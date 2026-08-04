package com.bp20.backend.api.effectverification.dto.response;

import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class EffectVerificationResponse {

    @JsonProperty("store_id")
    private Long storeId;

    @JsonProperty("recommendation_id")
    private String recommendationId;

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

    // AI가 지표를 사람이 읽는 문장으로 요약한 보고서(headline/summary/sections) — analysis_id 비교
    // 경로(verify-from-analyses)에서만 채워지고, 그 외에는 null일 수 있다.
    @JsonProperty("strategy_report")
    private Map<String, Object> strategyReport;
}
