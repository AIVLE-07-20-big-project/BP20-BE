package com.bp20.backend.api.effectverification.domain;

import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "EffectVerificationResult")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EffectVerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EffectVerificationResultID")
    private Long effectVerificationResultId;

    @Column(name = "AIRecommendationID", nullable = false, unique = true)
    private Long aiRecommendationId;

    @Column(name = "UserID")
    private Long userId;

    @Column(name = "StoreID", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "RecommendationType", nullable = false, length = 20)
    private RecommendationType recommendationType;

    @Column(name = "EffectScore", nullable = false)
    private Double effectScore;

    @Column(name = "Verdict", nullable = false, length = 30)
    private String verdict;

    @Lob
    @Column(name = "MetricResults", columnDefinition = "JSON")
    private String metricResults;

    @Column(name = "Summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "VerifiedDate", nullable = false)
    private LocalDateTime verifiedDate;

    public void update(
            Long storeId,
            RecommendationType recommendationType,
            Double effectScore,
            String verdict,
            String metricResults,
            String summary,
            LocalDateTime verifiedDate
    ) {
        this.storeId = storeId;
        this.recommendationType = recommendationType;
        this.effectScore = effectScore;
        this.verdict = verdict;
        this.metricResults = metricResults;
        this.summary = summary;
        this.verifiedDate = verifiedDate;
    }
}
