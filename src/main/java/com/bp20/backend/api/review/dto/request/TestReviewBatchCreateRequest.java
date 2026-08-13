package com.bp20.backend.api.review.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TestReviewBatchCreateRequest(
        @NotEmpty(message = "최소 한 건의 리뷰가 필요합니다.")
        List<@Valid TestReviewCreateRequest> reviews
) {
}
