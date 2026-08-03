package com.bp20.backend.api.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewItemDto(
    @JsonProperty("review_id") Long reviewId,
    @JsonProperty("review_text") String reviewText
) {}
