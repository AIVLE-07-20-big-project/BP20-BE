package com.bp20.backend.api.salestarget.repository;

import com.bp20.backend.api.salestarget.domain.SalesTargetBatchRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SalesTargetBatchRunRepository extends JpaRepository<SalesTargetBatchRun, String> {
    List<SalesTargetBatchRun> findAllByOrderByCreatedAtDesc();

    // 4단계(운영 정리 정책) — SalesTargetBatchService.autoRejectStaleBatches()가 N일 이상 지난
    // 배치 실행을 찾을 때 쓴다. 대기/완료/반려 여부는 resultJson 안에 있어 DB 쿼리로는 못 거르므로,
    // 일단 날짜로만 걸러서 가져온 뒤 서비스 레이어에서 대기 중인 것만 추려낸다.
    List<SalesTargetBatchRun> findAllByCreatedAtBefore(LocalDateTime threshold);
}
