package com.bp20.backend.api.store.dto.response;

import com.bp20.backend.api.store.domain.StoreReviewKeyword;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record StoreReviewKeywordResponseDto(
        Long reviewKeywordId,
        String aspect,
        String sentiment,
        String keyword,
        Integer count,
        Double changeRate,
        LocalDateTime analyzedAt,
        @JsonProperty("matched_review_ids") List<Long> matchedReviewIds
) {
    public static StoreReviewKeywordResponseDto of(StoreReviewKeyword entity, Double changeRate) {
        return new StoreReviewKeywordResponseDto(
                entity.getId(),
                entity.getAspect(),
                entity.getSentiment(),
                entity.getKeyword(),
                entity.getCount(),
                changeRate,
                entity.getAnalyzedAt(),
                entity.getMatchedReviewIds()
        );
    }
}
