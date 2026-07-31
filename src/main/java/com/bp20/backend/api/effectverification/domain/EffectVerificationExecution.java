package com.bp20.backend.api.effectverification.domain;

import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.effectverification.dto.request.RecommendationType;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "EffectVerificationExecution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EffectVerificationExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EffectVerificationExecutionID")
    private Long effectVerificationExecutionId;

    @Column(name = "AIRecommendationID", nullable = false, unique = true, length = 64)
    private String aiRecommendationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RecommendationThreadID", unique = true)
    private AiRecommendationRun recommendationRun;

    @Column(name = "CampaignDecisionID", unique = true, length = 36)
    private String decisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StoreID", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "RecommendationType", nullable = false, length = 20)
    private RecommendationType recommendationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private VerificationStatus status;

    @Column(name = "ConditionJson", nullable = false, columnDefinition = "TEXT")
    private String conditionJson;

    @Column(name = "BeforeMetricsJson", nullable = false, columnDefinition = "TEXT")
    private String beforeMetricsJson;

    @Column(name = "SelectedActionJson", columnDefinition = "TEXT")
    private String selectedActionJson;

    @Column(name = "AfterMetricsJson", columnDefinition = "TEXT")
    private String afterMetricsJson;

    @Column(name = "ExecutedAt", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "VerificationDueAt", nullable = false)
    private LocalDateTime verificationDueAt;

    @Column(name = "VerifiedAt")
    private LocalDateTime verifiedAt;

    @Column(name = "FailureReason", columnDefinition = "TEXT")
    private String failureReason;

    @Builder.Default
    @Column(name = "AttemptCount", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "LastAttemptAt")
    private LocalDateTime lastAttemptAt;

    public void markReady(String afterMetricsJson) {
        this.afterMetricsJson = afterMetricsJson;
        this.status = VerificationStatus.READY;
        this.failureReason = null;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.status = VerificationStatus.VERIFIED;
        this.verifiedAt = verifiedAt;
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = VerificationStatus.FAILED;
        this.failureReason = failureReason;
    }

    public void beginAttempt(LocalDateTime attemptedAt) {
        this.attemptCount = this.attemptCount == null ? 1 : this.attemptCount + 1;
        this.lastAttemptAt = attemptedAt;
    }

    public void linkDecision(String decisionId) {
        this.decisionId = decisionId;
    }
}
