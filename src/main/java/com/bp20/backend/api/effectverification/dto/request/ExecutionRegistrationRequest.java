package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @Size(max = 64)
    @JsonProperty("recommendation_id")
    private String recommendationId;

    @Size(max = 36)
    @JsonProperty("thread_id")
    private String threadId;

    @Size(max = 36)
    @JsonProperty("decision_id")
    private String decisionId;

    @NotNull
    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    @NotNull
    @Valid
    private VerificationCondition condition;

    @NotNull
    @Valid
    private PeriodMetrics before;

    @Valid
    @JsonProperty("selected_action")
    private SelectedActionRequest selectedAction;

    @JsonProperty("executed_at")
    private LocalDateTime executedAt;
}
