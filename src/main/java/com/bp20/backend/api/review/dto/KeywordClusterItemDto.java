package com.bp20.backend.api.review.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KeywordClusterItemDto(
        @JsonProperty("aspect") String aspect,
        @JsonProperty("sentiment") String sentiment,
        @JsonProperty("representative_keyword") String representativeKeyword,
        @JsonProperty("count") Integer count,
        @JsonProperty("matched_review_ids") List<Long> matchedReviewIds,
        @JsonProperty("original_expressions") List<String> originalExpressions
) {}