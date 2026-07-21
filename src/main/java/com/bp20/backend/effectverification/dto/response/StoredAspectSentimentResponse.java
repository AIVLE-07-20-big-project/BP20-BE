package com.bp20.backend.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StoredAspectSentimentResponse(
        String aspect,
        String sentiment,
        Double confidence,
        @JsonProperty("sentiment_score") Double sentimentScore
) {
}

