package com.bp20.backend.effectverification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReviewBatchAnalysisResponse(
        @JsonProperty("analyzed_count") int analyzedCount,
        List<ReviewAnalysisStorageResponse> results
) {
}

