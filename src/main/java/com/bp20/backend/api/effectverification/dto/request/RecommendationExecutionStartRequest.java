package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
