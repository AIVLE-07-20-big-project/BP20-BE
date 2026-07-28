package com.bp20.backend.api.review.dto.response;

import com.bp20.backend.api.review.domain.Review;

import java.time.LocalDateTime;

public record ReviewResponseDto(
        Long id,
        Double rating,
        String content,
        LocalDateTime reviewedDate,
        Boolean isAnalyzed
) {
    public static ReviewResponseDto from(Review review) {
        return new ReviewResponseDto(
                review.getId(),
                review.getRating(),
                review.getContent(),
                review.getReviewedDate(),
                review.isAnalyzed()
        );
    }
}
