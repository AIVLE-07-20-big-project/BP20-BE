package com.bp20.backend.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewSentimentResponse(
        @JsonProperty("review_text") String reviewText,
        List<AspectSentimentResponse> results
) {
}

