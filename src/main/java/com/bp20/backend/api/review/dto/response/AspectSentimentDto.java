package com.bp20.backend.api.review.dto.response;

public record AspectSentimentDto(
        String aspect,
        String sentiment,
        Double confidence
) {}