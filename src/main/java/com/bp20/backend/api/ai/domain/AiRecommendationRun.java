package com.bp20.backend.api.ai.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_recommendation_runs", indexes = {
        @Index(name = "idx_ai_runs_user", columnList = "user_id"),
        @Index(name = "idx_ai_runs_analysis", columnList = "analysis_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendationRun extends BaseTimeEntity {
    @Id @Column(name = "thread_id", length = 36)
    private String threadId;
    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Lob @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

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
}
