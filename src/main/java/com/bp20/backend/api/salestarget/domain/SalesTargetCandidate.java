package com.bp20.backend.api.salestarget.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 신규 가맹점 영업 타겟 추천 배치 결과 1건. AI 서버가 계산한 점수와, 영업팀이 관리하는
 * 파이프라인 상태(pipelineStatus)를 함께 갖는다 — 후자는 배치가 다시 돌아도 덮어쓰지 않는다
 * (SalesTargetService의 벌크 업서트 로직 참고).
 *
 * businessName + address 조합을 자연키로 취급한다. 공공데이터에 사업자등록번호가 없어서
 * 이 두 값의 조합으로 "같은 후보 업장"을 여러 배치 실행 사이에서 식별한다 — 완벽하지 않다
 * (같은 이름/주소인데 실제로는 다른 업장일 여지, 도로명/지번 표기 차이로 같은 곳을 다른 걸로
 * 인식할 여지가 있음). 오매칭이 확인되면 나중에 공공데이터의 상가업소번호(bizesId) 같은 안정적인
 * 키로 바꾸는 걸 검토해야 한다.
 */
@Entity
@Table(name = "sales_target_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SalesTargetCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    private String industry;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double totalScore;

    private Double growthScore;

    private Double trafficScore;

    private Double reviewScore;

    private Double similarityScore;

    @Column(columnDefinition = "TEXT")
    private String salesPitch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PipelineStatus pipelineStatus;

    // 이 후보를 만들어낸 배치 실행 식별자. 이력 추적/디버깅용이고 현재 조회 로직이 이 값으로
    // 필터링하진 않는다(항상 최신 점수를 유지하는 단일 테이블 방식 — 자세한 이유는
    // 우수가맹점/BE결과저장 적용방법.md 참고).
    private String sourceBatchId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.pipelineStatus == null) {
            this.pipelineStatus = PipelineStatus.CANDIDATE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePipelineStatus(PipelineStatus newStatus) {
        this.pipelineStatus = newStatus;
    }

    /**
     * 새 배치 결과로 점수/업종/피칭문구를 갱신한다. pipelineStatus는 건드리지 않는다 —
     * 영업팀이 이미 진행 중인 상태를 배치 재계산이 되돌리면 안 되기 때문이다.
     */
    public void refreshScores(
            String industry,
            Double totalScore,
            Double growthScore,
            Double trafficScore,
            Double reviewScore,
            Double similarityScore,
            String sourceBatchId
    ) {
        this.industry = industry;
        this.totalScore = totalScore;
        this.growthScore = growthScore;
        this.trafficScore = trafficScore;
        this.reviewScore = reviewScore;
        this.similarityScore = similarityScore;
        this.sourceBatchId = sourceBatchId;
    }
}
