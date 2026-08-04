package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationExecutionStartRequest {

    @NotBlank
    @Size(max = 36)
    @JsonProperty("thread_id")
    private String threadId;

    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    @Size(max = 100)
    @JsonProperty("target_aspect")
    private String targetAspect;

    // 사용자가 추천 승인 화면에서 고른 적용 기간 — 없으면 지금 시각부터 14일로 기본 동작한다.
    @JsonProperty("execution_started_at")
    private LocalDateTime executionStartedAt;

    @JsonProperty("execution_ended_at")
    private LocalDateTime executionEndedAt;
}
