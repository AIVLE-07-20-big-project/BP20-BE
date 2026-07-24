package com.bp20.backend.api.review.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewAnalysisRequestDto (
    @JsonProperty("review_id") Long reviewId,
    @JsonProperty("review_text") String reviewText
) {}
