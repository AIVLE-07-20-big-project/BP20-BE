package com.bp20.backend.api.reviewAnalysis.dto.response;

public record AspectSentimentDto(
        String aspect,
        String sentiment,
        Double confidence
) {}