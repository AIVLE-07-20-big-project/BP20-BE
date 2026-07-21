package com.bp20.backend.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewAnalysisStorageResponse(
        @JsonProperty("review_id") Long reviewId,
        @JsonProperty("review_text") String reviewText,
        List<StoredAspectSentimentResponse> results
) {
}

