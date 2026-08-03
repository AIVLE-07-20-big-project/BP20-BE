package com.bp20.backend.api.review.dto.response;

import com.bp20.backend.api.review.dto.KeywordClusterItemDto;
import com.bp20.backend.api.review.dto.ReviewAnalysisDto;
import com.bp20.backend.api.store.dto.response.FastAPIRecommendationResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ABSAAgentResponseDto (
        @JsonProperty("store_id") Long storeId,
        @JsonProperty("summary") String summary,
        @JsonProperty("total_reviews_analyzed") Integer totalReviewsAnalyzed,
        @JsonProperty("reviews_analysis") List<ReviewAnalysisDto> reviewsAnalysis,
        @JsonProperty("clusters") List<KeywordClusterItemDto> clusters,
        @JsonProperty("improvement_report") FastAPIRecommendationResponseDto.ImprovementReportDto improvementReport
) {}