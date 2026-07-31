package com.bp20.backend.api.review.dto.request;

import com.bp20.backend.api.review.dto.ReviewItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BatchReviewRequestDto(
        @JsonProperty("store_id") Long storeId,
        @JsonProperty("reviews") List<ReviewItemDto> reviews
) {
}
