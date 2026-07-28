package com.bp20.backend.api.effectverification.dto.request;

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
public class MockThreadExecutionRegistrationRequest {

    @NotNull
    @Positive
    @JsonProperty("store_id")
    private Long storeId;

    @NotNull
    @JsonProperty("recommendation_type")
    private RecommendationType recommendationType;

    @NotNull
    @Valid
    private VerificationCondition condition;

    @JsonProperty("executed_at")
    private LocalDateTime executedAt;
}
