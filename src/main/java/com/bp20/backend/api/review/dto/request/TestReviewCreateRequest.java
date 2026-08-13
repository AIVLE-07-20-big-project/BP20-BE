package com.bp20.backend.api.review.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TestReviewCreateRequest(
        @NotNull(message = "평점은 필수입니다.")
        @DecimalMin(value = "0.5", message = "평점은 0.5점 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5점 이하여야 합니다.")
        Double rating,

        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 5000, message = "리뷰 내용은 5,000자를 초과할 수 없습니다.")
        String content,

        @PastOrPresent(message = "리뷰 작성일은 현재보다 미래일 수 없습니다.")
        LocalDateTime reviewedDate
) {
}
