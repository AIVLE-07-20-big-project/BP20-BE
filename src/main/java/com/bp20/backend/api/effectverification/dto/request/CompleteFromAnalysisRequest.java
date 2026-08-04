package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 승인된 추천(thread)에 대해 적용후 분석 결과로 매출형 전략검증을 완료 요청한다 */
@Getter
@Setter
@NoArgsConstructor
public class CompleteFromAnalysisRequest {

    @NotBlank
    @JsonProperty("after_analysis_id")
    private String afterAnalysisId;
}
