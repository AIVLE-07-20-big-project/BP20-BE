package com.bp20.backend.api.review.dto;

import java.util.List;

public record ReviewAnalysisDto(
        Long reviewId,
        List<AspectSentimentDto> results
) {}
