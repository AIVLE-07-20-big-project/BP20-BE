package com.bp20.backend.api.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CampaignLogRequest(
        @NotBlank @JsonProperty("thread_id") String threadId,
        @NotNull Boolean executed,
        @NotNull @JsonProperty("treatment_yyqu_cd") Integer treatmentYyquCd,
        @JsonProperty("revenue_after") Double revenueAfter
) {
}
