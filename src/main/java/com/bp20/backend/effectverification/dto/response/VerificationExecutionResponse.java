package com.bp20.backend.effectverification.dto.response;

import com.bp20.backend.effectverification.dto.request.RecommendationType;
import com.bp20.backend.effectverification.entity.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VerificationExecutionResponse {

    @JsonProperty("store_id")
    private Long storeId;

    @JsonProperty("recommendation_id")
    private Long recommendationId;

    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    private VerificationStatus status;

    @JsonProperty("executed_at")
    private LocalDateTime executedAt;

    @JsonProperty("verification_due_at")
    private LocalDateTime verificationDueAt;

    @JsonProperty("verified_at")
    private LocalDateTime verifiedAt;

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("attempt_count")
    private Integer attemptCount;

    @JsonProperty("last_attempt_at")
    private LocalDateTime lastAttemptAt;
}
