package com.bp20.backend.api.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "store_review_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreReviewRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_recommendation_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "executive_summary", nullable = false, columnDefinition = "TEXT")
    private String executiveSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_items", columnDefinition = "json", nullable = false)
    private List<ActionItem> actionItems;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StoreReviewRecommendation(Long storeId, String executiveSummary, List<ActionItem> actionItems) {
        this.storeId = storeId;
        this.executiveSummary = executiveSummary;
        this.actionItems = actionItems;
        this.createdAt = LocalDateTime.now();
    }

    public record ActionItem(
            String priority,
            String aspect,
            String keyword,
            String trendSummary,
            String problemCause,
            String actionPlan,
            String expectedOutcome,
            LocalDateTime executedAt
    ) {}
}