package com.bp20.backend.api.review.dto.response;

public record ReviewTrendResponseDto(
        String week,
        Double averageRating,
        Long negativeReviewCount
) {
}
