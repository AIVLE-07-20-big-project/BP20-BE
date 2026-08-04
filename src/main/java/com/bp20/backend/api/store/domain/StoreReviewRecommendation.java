package com.bp20.backend.api.store.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "store_review_recommendation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreReviewRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "executive_summary", nullable = false, columnDefinition = "TEXT")
    private String executiveSummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_items", columnDefinition = "json", nullable = false)
    private List<ActionItem> actionItems;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StoreReviewRecommendation(Store store, String executiveSummary, List<ActionItem> actionItems) {
        this.store = store;
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
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime executedAt
    ) {}
}