package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReviewSentimentRequest(
        @JsonProperty("review_text") String reviewText
) {
}

