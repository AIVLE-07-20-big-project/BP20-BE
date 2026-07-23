package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class VerificationCompletionRequest {

    @NotNull
    @Valid
    private PeriodMetrics after;

    @JsonProperty("collected_at")
    private LocalDateTime collectedAt;
}
