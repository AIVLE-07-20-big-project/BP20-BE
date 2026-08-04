package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** /analyses로 이미 저장된 적용전·적용후 분석 두 건을 비교하는 AI 매출형 전략검증 요청 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EffectVerificationFromAnalysesRequest {

    @JsonProperty("before_analysis_id")
    private String beforeAnalysisId;

    @JsonProperty("after_analysis_id")
    private String afterAnalysisId;

    @JsonProperty("store_id")
    private Long storeId;

    // AI 서비스 스키마가 int를 요구한다 — thread_id 같은 UUID 문자열은 보낼 수 없다.
    @JsonProperty("recommendation_id")
    private Long recommendationId;

    @JsonProperty("start_hour")
    private Integer startHour;

    @JsonProperty("end_hour")
    private Integer endHour;

    @JsonProperty("period_days")
    private Integer periodDays;
}
