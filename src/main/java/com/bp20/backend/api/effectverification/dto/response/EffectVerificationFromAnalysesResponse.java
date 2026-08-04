package com.bp20.backend.api.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 저장된 분석 결과(적용전·적용후) 두 건으로 자동 집계한 매출형 전략검증 응답 */
@Getter
@Setter
@NoArgsConstructor
public class EffectVerificationFromAnalysesResponse extends EffectVerificationResponse {

    @JsonProperty("computation_notes")
    private List<String> computationNotes;
}
