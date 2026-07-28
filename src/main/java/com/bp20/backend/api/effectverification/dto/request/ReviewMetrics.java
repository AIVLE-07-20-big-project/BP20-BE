package com.bp20.backend.api.effectverification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMetrics {

    @JsonProperty("average_rating")
    private Double averageRating;

    @JsonProperty("negative_review_rate")
    private Double negativeReviewRate;

    @JsonProperty("target_aspect_review_count")
    private Integer targetAspectReviewCount;

    @JsonProperty("target_aspect_negative_rate")
    private Double targetAspectNegativeRate;

    @JsonProperty("target_aspect_average_confidence")
    private Double targetAspectAverageConfidence;

    @JsonProperty("review_count")
    private Integer reviewCount;

    @JsonProperty("revisit_rate")
    private Double revisitRate;

    private Double sales;
}