package com.bp20.backend.api.salestarget.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 서버(app/sales_target/graph.py) LangGraph 배치 실행 1건의 BE 쪽 추적 레코드.
 *
 * api/ai/domain/AiRecommendationRun과 동일한 목적(threadId 기준으로 최근 조회한 AI 응답을
 * 캐시해두면, 관리자가 새로고침해도 다시 AI를 호출하지 않고 바로 "승인 대기 중" 배너를 그릴 수
 * 있음)이다. 차이는 userId 대신 triggeredByAdminId를 쓴다는 것뿐 — 영업 타겟 배치는 특정
 * 점주가 아니라 관리자가 트리거하는 회사 전체 단위 작업이라 조회도 트리거한 사람으로 한정하지
 * 않는다(findAllByOrderByCreatedAtDesc, 아무 관리자나 진행 상황을 볼 수 있어야 함).
 *
 * 실제 후보 데이터의 단일 진실 소스는 아니다 — 승인 후 finalize 노드가 BE의
 * /api/internal/sales-targets/bulk로 반영한 sales_target_candidates 테이블이 진짜 저장소이고,
 * 이 테이블은 "지금 어떤 배치가 승인 대기 중인지"만 추적하는 캐시성 로그다.
 *
 * autoRejected: AI_Agent_전환_가이드라인.md 4단계(운영 정리 정책) — 관리자가 N일 이상 승인/반려
 * 하지 않고 방치한 배치를 SalesTargetBatchService.autoRejectStaleBatches()가 대신 반려 처리했을
 * 때 true로 표시한다. AI 쪽엔 이 구분이 없다(같은 /reject 엔드포인트를 호출) — FE가 "관리자가
 * 반려함"과 "방치되어 자동 반려됨"을 구분해서 보여줄 수 있게 BE가 별도로 추적하는 값이다.
 */
@Getter
@Entity
@Table(name = "sales_target_batch_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesTargetBatchRun extends BaseTimeEntity {

    @Id
    @Column(name = "thread_id", length = 36)
    private String threadId;

    @Column(name = "triggered_by_admin_id", nullable = false)
    private Long triggeredByAdminId;

    @Lob
    @Column(name = "result_json", nullable = false, columnDefinition = "LONGTEXT")
    private String resultJson;

    @Column(name = "auto_rejected", nullable = false)
    private boolean autoRejected;

    private SalesTargetBatchRun(String threadId, Long triggeredByAdminId, String resultJson) {
        this.threadId = threadId;
        this.triggeredByAdminId = triggeredByAdminId;
        this.resultJson = resultJson;
        this.autoRejected = false;
    }

    public static SalesTargetBatchRun create(String threadId, Long triggeredByAdminId, String resultJson) {
        return new SalesTargetBatchRun(threadId, triggeredByAdminId, resultJson);
    }

    public void updateResult(String resultJson) {
        this.resultJson = resultJson;
    }

    public void markAutoRejected() {
        this.autoRejected = true;
    }
}
