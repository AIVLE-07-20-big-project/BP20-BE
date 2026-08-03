package com.bp20.backend.api.review.domain;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(nullable = false)
    private String aspect;

    @Column(nullable = false)
    private String sentiment;

    private Double confidence;

    @Builder
    public ReviewAnalysis(Review review, String aspect, String sentiment, Double confidence) {
        this.review = review;
        this.aspect = aspect;
        this.sentiment = sentiment;
        this.confidence = confidence;
    }
}
