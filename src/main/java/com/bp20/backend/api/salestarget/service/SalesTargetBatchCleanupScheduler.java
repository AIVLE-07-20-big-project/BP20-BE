package com.bp20.backend.api.salestarget.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI_Agent_전환_가이드라인.md 4단계(운영 정리 정책) — interrupt에서 멈춘 채 아무도 승인/반려하지
 * 않는 thread가 쌓이는 걸 막기 위해, staleDays일 이상 방치된 배치를 주기적으로 자동 반려한다.
 *
 * ProductionVerificationScheduler(effect-verification)와 동일한 패턴(@Value로 cron/활성화
 * 여부를 주입, 로직 자체는 서비스에 위임)을 따른다.
 */
@Service
@RequiredArgsConstructor
public class SalesTargetBatchCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SalesTargetBatchCleanupScheduler.class);

    private final SalesTargetBatchService salesTargetBatchService;

    @Value("${sales-target.batch-cleanup.enabled:true}")
    private boolean enabled;

    @Value("${sales-target.batch-cleanup.stale-days:3}")
    private int staleDays;

    @Scheduled(
            cron = "${sales-target.batch-cleanup.cron:0 0 3 * * *}",
            zone = "${sales-target.batch-cleanup.zone:Asia/Seoul}"
    )
    public void runScheduled() {
        if (!enabled) {
            return;
        }
        List<String> rejected = salesTargetBatchService.autoRejectStaleBatches(staleDays);
        if (!rejected.isEmpty()) {
            log.info("영업 타겟 배치 자동 반려 스케줄러: {}건 반려 - {}", rejected.size(), rejected);
        }
    }
}
