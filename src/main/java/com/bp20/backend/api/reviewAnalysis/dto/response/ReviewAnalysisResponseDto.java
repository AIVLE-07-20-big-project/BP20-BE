package com.bp20.backend.api.reviewAnalysis.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewAnalysisResponseDto(
        @JsonProperty("review_id") Long reviewId,
        List<AspectSentimentDto> results
) {}
