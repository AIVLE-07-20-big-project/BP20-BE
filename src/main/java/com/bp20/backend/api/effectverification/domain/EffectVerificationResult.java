package com.bp20.backend.api.effectverification.domain;

import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.User;
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

    @Column(name = "AIRecommendationID", nullable = false, unique = true, length = 64)
    private String aiRecommendationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EffectVerificationExecutionID", unique = true)
    private EffectVerificationExecution execution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StoreID", nullable = false)
    private Store store;

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
            Store store,
            RecommendationType recommendationType,
            Double effectScore,
            String verdict,
            String metricResults,
            String summary,
            LocalDateTime verifiedDate
    ) {
        this.store = store;
        this.recommendationType = recommendationType;
        this.effectScore = effectScore;
        this.verdict = verdict;
        this.metricResults = metricResults;
        this.summary = summary;
        this.verifiedDate = verifiedDate;
    }

    public void linkExecution(EffectVerificationExecution execution) {
        this.execution = execution;
    }
}
