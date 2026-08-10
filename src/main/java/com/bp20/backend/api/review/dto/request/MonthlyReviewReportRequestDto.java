package com.bp20.backend.api.review.dto.request;

import com.bp20.backend.api.review.dto.MonthlyPreclassifiedResultDto;
import com.bp20.backend.api.review.dto.ReviewItemDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MonthlyReviewReportRequestDto(
        @JsonProperty("store_id") Long storeId,
        List<ReviewItemDto> reviews,
        @JsonProperty("preclassified_results") List<MonthlyPreclassifiedResultDto> preclassifiedResults
) {}
