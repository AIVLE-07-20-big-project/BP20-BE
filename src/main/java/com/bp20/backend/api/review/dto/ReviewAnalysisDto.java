package com.bp20.backend.api.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ReviewAnalysisDto(
        @JsonProperty("review_id") Long reviewId,
        @JsonProperty("results") List<AspectSentimentDto> results
) {}