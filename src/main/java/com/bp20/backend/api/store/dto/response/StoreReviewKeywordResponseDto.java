package com.bp20.backend.api.review.dto;

import java.util.List;

public record KeywordClusterDto(
        String aspect,
        String sentiment,
        String representative,
        Integer count,
        List<Long> matchedReviewIds,
        List<String> originalExpressions
) {
}
