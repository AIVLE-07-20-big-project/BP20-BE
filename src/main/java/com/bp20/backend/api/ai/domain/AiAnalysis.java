package com.bp20.backend.api.ai.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ai_analyses", indexes = {
        @Index(name = "idx_ai_analyses_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_ai_analyses_store", columnList = "store_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiAnalysis extends BaseTimeEntity {
    @Id @Column(name = "analysis_id", length = 36)
    private String analysisId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "store_id", length = 100)
    private String storeId;
    @Column(name = "trdar_cd", nullable = false, length = 30)
    private String trdarCd;
    @Column(name = "svc_induty_cd", nullable = false, length = 30)
    private String svcIndutyCd;
    @Column(name = "yyqu_cd")
    private Integer yyquCd;
    @Lob @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    private AiAnalysis(String analysisId, Long userId, String storeId, String trdarCd,
                       String svcIndutyCd, Integer yyquCd, String resultJson) {
        this.analysisId = analysisId;
        this.userId = userId;
        this.storeId = storeId;
        this.trdarCd = trdarCd;
        this.svcIndutyCd = svcIndutyCd;
        this.yyquCd = yyquCd;
        this.resultJson = resultJson;
    }

    public static AiAnalysis create(String analysisId, Long userId, String storeId, String trdarCd,
                                    String svcIndutyCd, Integer yyquCd, String resultJson) {
        return new AiAnalysis(analysisId, userId, storeId, trdarCd, svcIndutyCd, yyquCd, resultJson);
    }
}
