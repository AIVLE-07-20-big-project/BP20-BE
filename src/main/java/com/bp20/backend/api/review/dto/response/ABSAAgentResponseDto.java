package com.bp20.backend.api.review.dto.response;

public record ABSAAgentResponseDto (
    Long storeId,
    String summary,
    Integer totalReviewsAnalyzed,
    List<ReviewAnalysisDto> reviewsAnalysis,
    List<KeywordClusterDto> clusters
) {}
