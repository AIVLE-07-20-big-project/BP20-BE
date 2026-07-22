package com.bp20.backend.api.effectverification.dto.response;

public record AspectSentimentResponse(
        String aspect,
        String sentiment,
        Double confidence
) {
}

