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
        @Index(name = "idx_ai_runs_analysis", columnList = "analysis_id"),
        @Index(name = "idx_ai_runs_store", columnList = "store_id"),
        // 추천 이력 조회: 사용자/매장별 최신 실행순
        @Index(name = "idx_ai_runs_user_store_created", columnList = "user_id, store_id, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRecommendationRun extends BaseTimeEntity {
    @Id @Column(name = "thread_id", length = 36)
    private String threadId;
    @Column(name = "analysis_id", nullable = false, length = 36)
    private String analysisId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "store_id", length = 100)
    private String storeId;
    @Lob @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    private AiRecommendationRun(String threadId, String analysisId, Long userId, String storeId, String resultJson) {
        this.threadId = threadId;
        this.analysisId = analysisId;
        this.userId = userId;
        this.storeId = storeId;
        this.resultJson = resultJson;
    }

    public static AiRecommendationRun create(String threadId, String analysisId,
                                             Long userId, String storeId, String resultJson) {
        return new AiRecommendationRun(threadId, analysisId, userId, storeId, resultJson);
    }

    public void updateResult(String resultJson) {
        this.resultJson = resultJson;
    }
}
