package com.bp20.backend.api.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MonthlyPreclassifiedResultDto(
        @JsonProperty("review_id") Long reviewId,
        String aspect,
        String sentiment,
        Double confidence,
        @JsonProperty("review_text") String reviewText
) {}
