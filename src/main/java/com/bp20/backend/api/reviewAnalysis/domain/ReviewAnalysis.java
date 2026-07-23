package com.bp20.backend.api.reviewAnalysis.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_analysis_id")
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "aspect", nullable = false)
    private String aspect;

    @Column(name = "sentiment", nullable = false)
    private String sentiment;

    @Column(name = "confidence")
    private Double confidence;

    @Builder
    public ReviewAnalysis(Long reviewId, String aspect, String sentiment, Double confidence) {
        this.reviewId = reviewId;
        this.aspect = aspect;
        this.sentiment = sentiment;
        this.confidence = confidence;
    }
}
