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
@Table(name = "ai_recommendation_runs", indexes = {
        @Index(name = "idx_ai_runs_user", columnList = "user_id"),
        @Index(name = "idx_ai_runs_analysis", columnList = "analysis_id"),
        // 추천 이력 조회: 사용자별 최신 실행순
        @Index(name = "idx_ai_runs_user_created", columnList = "user_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendationRun extends BaseTimeEntity {
    @Id @Column(name = "thread_id", length = 36)
    private String threadId;
    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_runs_analysis"))
    private AiAnalysis analysis;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_ai_runs_user"))
    private User user;
    @Lob @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;
    @Column(name = "execution_started_at")
    private LocalDateTime executionStartedAt;
    @Column(name = "execution_ended_at")
    private LocalDateTime executionEndedAt;
    @Column(name = "selected_action", length = 100)
    private String selectedAction;

    private AiRecommendationRun(String threadId, String analysisId, Long userId, String resultJson) {
        this.threadId = threadId;
        this.analysisId = analysisId;
        this.userId = userId;
        this.resultJson = resultJson;
    }

    public static AiRecommendationRun create(String threadId, String analysisId,
                                             Long userId, String resultJson) {
        return new AiRecommendationRun(threadId, analysisId, userId, resultJson);
    }

    public void updateResult(String resultJson) {
        this.resultJson = resultJson;
    }

    public void updateExecutionPlan(
            LocalDateTime startedAt, LocalDateTime endedAt, String action
    ) {
        this.executionStartedAt = startedAt;
        this.executionEndedAt = endedAt;
        this.selectedAction = action;
    }
}
