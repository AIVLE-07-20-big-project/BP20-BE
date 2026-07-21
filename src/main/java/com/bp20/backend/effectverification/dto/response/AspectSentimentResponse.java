package com.bp20.backend.effectverification.dto.response;

public record AspectSentimentResponse(
        String aspect,
        String sentiment,
        Double confidence
) {
}

