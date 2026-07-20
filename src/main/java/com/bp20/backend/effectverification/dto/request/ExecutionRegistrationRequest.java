package com.bp20.backend.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ExecutionRegistrationRequest {

    @NotNull
    @Positive
    @JsonProperty("store_id")
    private Long storeId;

    @NotNull
    @Positive
    @JsonProperty("recommendation_id")
    private Long recommendationId;

    @NotNull
    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    @NotNull
    @Valid
    private VerificationCondition condition;

    @NotNull
    @Valid
    private PeriodMetrics before;

    @JsonProperty("executed_at")
    private LocalDateTime executedAt;
}
