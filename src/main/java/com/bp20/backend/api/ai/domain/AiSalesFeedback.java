package com.bp20.backend.api.ai.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "ai_sales_feedback", indexes = {
        @Index(name = "idx_ai_feedback_user", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_feedback_thread", columnList = "thread_id", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSalesFeedback extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "thread_id", nullable = false, unique = true, length = 36)
    private String threadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_feedback_thread"))
    private AiRecommendationRun recommendationRun;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_feedback_user"))
    private User user;
    @Column(name = "selected_action", length = 100)
    private String selectedAction;
    @Column(name = "execution_started_at", nullable = false)
    private LocalDateTime executionStartedAt;
    @Column(name = "execution_ended_at", nullable = false)
    private LocalDateTime executionEndedAt;
    @Column(name = "before_sales", nullable = false)
    private Double beforeSales;
    @Column(name = "after_sales", nullable = false)
    private Double afterSales;
    @Column(name = "change_rate", nullable = false)
    private Double changeRate;
    @Column(name = "learned", nullable = false)
    private boolean learned;

    private AiSalesFeedback(
            String threadId, Long userId, String selectedAction,
            LocalDateTime executionStartedAt, LocalDateTime executionEndedAt,
            double beforeSales, double afterSales, double changeRate
    ) {
        this.threadId = threadId;
        this.userId = userId;
        this.selectedAction = selectedAction;
        this.executionStartedAt = executionStartedAt;
        this.executionEndedAt = executionEndedAt;
        this.beforeSales = beforeSales;
        this.afterSales = afterSales;
        this.changeRate = changeRate;
        this.learned = false;
    }

    public static AiSalesFeedback create(
            String threadId, Long userId, String selectedAction,
            LocalDateTime executionStartedAt, LocalDateTime executionEndedAt,
            double beforeSales, double afterSales, double changeRate
    ) {
        return new AiSalesFeedback(
                threadId, userId, selectedAction, executionStartedAt,
                executionEndedAt, beforeSales, afterSales, changeRate
        );
    }

    public void markLearned() {
        this.learned = true;
    }
}
