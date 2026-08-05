package com.bp20.backend.api.salestarget.service;

import com.bp20.backend.api.salestarget.client.SalesTargetAiClient;
import com.bp20.backend.api.salestarget.domain.SalesTargetBatchRun;
import com.bp20.backend.api.salestarget.repository.SalesTargetBatchRunRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 신규 가맹점 영업 타겟 배치(AI 서버 LangGraph 그래프)를 관리자 대신 실행/조회/승인/반려한다.
 *
 * AI_Agent_전환_가이드라인.md 4절의 권장대로, FE가 AI 서버를 직접 호출하지 않고 항상 BE를
 * 거치게 한다(설계가이드 결정 3 유지). 이 서비스는 api/ai/service/AiService의 agent-run
 * 프록시 패턴을 그대로 따른다 — AI가 돌려주는 응답(Map<String,Object>, 한글 키 포함)을 강한
 * 타입 DTO로 재포장하지 않고 그대로 통과시킨다. 승인/반려 이후의 실제 후보 데이터 반영은 AI의
 * finalize 노드가 /api/internal/sales-targets/bulk를 직접 호출해서 처리하므로(InternalSalesTargetIngestController),
 * 이 서비스가 따로 SalesTargetCandidate를 저장하지는 않는다.
 */
@Service
@RequiredArgsConstructor
public class SalesTargetBatchService {

    private static final Logger log = LoggerFactory.getLogger(SalesTargetBatchService.class);

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final SalesTargetAiClient salesTargetAiClient;
    private final SalesTargetBatchRunRepository batchRunRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> startBatch(Long adminId, Integer topN) {
        Map<String, Object> result = salesTargetAiClient.startBatch(topN);
        String threadId = requiredString(result, "thread_id");
        batchRunRepository.save(SalesTargetBatchRun.create(threadId, adminId, write(result)));
        return result;
    }

    public List<Map<String, Object>> listBatches() {
        return batchRunRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponseMap)
                .toList();
    }

    public Map<String, Object> getBatch(String threadId) {
        SalesTargetBatchRun run = findRun(threadId);
        Map<String, Object> result = salesTargetAiClient.getBatch(threadId);
        run.updateResult(write(result));
        return withAutoRejected(result, run);
    }

    public Map<String, Object> approveBatch(String threadId) {
        SalesTargetBatchRun run = findRun(threadId);
        Map<String, Object> result = salesTargetAiClient.approveBatch(threadId);
        run.updateResult(write(result));
        return result;
    }

    public Map<String, Object> rejectBatch(String threadId) {
        SalesTargetBatchRun run = findRun(threadId);
        Map<String, Object> result = salesTargetAiClient.rejectBatch(threadId);
        run.updateResult(write(result));
        return result;
    }

    /**
     * 배치 이력 한 줄을 정리용으로 지운다. 실제 후보 데이터(sales_target_candidates)는 이미
     * finalize 노드가 별도로 반영했으므로 안 건드리고, 이 서비스가 관리하는 캐시성 로그
     * (SalesTargetBatchRun)만 지운다.
     *
     * 아직 AI 쪽에서 승인 대기 중인 배치는 삭제를 막는다 — 여기서 지워버리면 LangGraph 쪽
     * thread는 계속 살아있는데 관리자 화면에서는 그 존재 자체가 안 보이게 되어(추적 불가능한
     * 대기 상태), 나중에 아무도 승인/반려하지 못하는 상태로 방치될 수 있기 때문이다. 삭제 전에
     * AI 쪽 최신 상태를 한 번 더 확인해서, 캐시가 stale하게 "대기 중"으로 남아있을 뿐인 경우는
     * 정상적으로 삭제되게 한다.
     */
    public void deleteBatchRun(String threadId) {
        SalesTargetBatchRun run = findRun(threadId);
        Map<String, Object> latest;
        try {
            latest = salesTargetAiClient.getBatch(threadId);
        } catch (RuntimeException e) {
            // AI 쪽에서 이미 못 찾는(404 등) 스레드라면 BE 캐시만 남은 고아 레코드이니 그냥 지운다.
            log.warn("배치 이력 삭제 전 상태 재조회 실패, 캐시만 지웁니다 - {}: {}", threadId, e.getMessage());
            batchRunRepository.delete(run);
            return;
        }
        run.updateResult(write(latest));
        if (isPending(latest)) {
            throw new ApiException(ErrorCode.CONFLICT_SALES_TARGET_BATCH_STILL_PENDING);
        }
        batchRunRepository.delete(run);
    }

    /**
     * AI_Agent_전환_가이드라인.md 4단계 — 관리자가 staleDays일 이상 승인/반려하지 않고 방치한
     * 배치를 대신 반려 처리한다. SalesTargetBatchCleanupScheduler(주기 실행)와 관리자 수동 트리거
     * 엔드포인트(POST .../cleanup) 양쪽에서 호출한다.
     *
     * 캐시된 resultJson만 보고 판단하지 않는다 — DB 쿼리(findAllByCreatedAtBefore)는 날짜로만
     * 거르므로, 그 사이 관리자가 이미 승인/반려했을 수 있다. 반려 직전에 AI에 실제 최신 상태를
     * 한 번 더 물어보고, 그래도 여전히 승인 대기 중일 때만 반려한다.
     *
     * @return 이번 호출에서 실제로 자동 반려한 thread_id 목록
     */
    public List<String> autoRejectStaleBatches(int staleDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(staleDays);
        List<SalesTargetBatchRun> candidates = batchRunRepository.findAllByCreatedAtBefore(cutoff);

        List<String> rejected = new ArrayList<>();
        for (SalesTargetBatchRun run : candidates) {
            if (!isPending(read(run.getResultJson()))) {
                continue;
            }

            Map<String, Object> latest;
            try {
                latest = salesTargetAiClient.getBatch(run.getThreadId());
            } catch (RuntimeException e) {
                log.warn("자동 반려 정책: {} 상태 재조회 실패, 이번 회차는 건너뜀 - {}", run.getThreadId(), e.getMessage());
                continue;
            }
            run.updateResult(write(latest));
            if (!isPending(latest)) {
                continue;
            }

            Map<String, Object> result = salesTargetAiClient.rejectBatch(run.getThreadId());
            run.updateResult(write(result));
            run.markAutoRejected();
            rejected.add(run.getThreadId());
            log.info("자동 반려 정책: {}일 이상 방치된 배치 {}를 자동 반려했습니다.", staleDays, run.getThreadId());
        }
        return rejected;
    }

    private boolean isPending(Map<String, Object> result) {
        return result.get("대기중_승인") != null;
    }

    private Map<String, Object> toResponseMap(SalesTargetBatchRun run) {
        Map<String, Object> result = read(run.getResultJson());
        result.putIfAbsent("thread_id", run.getThreadId());
        return withAutoRejected(result, run);
    }

    private Map<String, Object> withAutoRejected(Map<String, Object> result, SalesTargetBatchRun run) {
        // result가 Map.of(...)류 불변 맵일 수 있어(AI client 응답, 테스트 목 등) 그대로 put하지
        // 않고 새 맵으로 복사한다. FE 배치 이력 화면(4단계)이 "언제 시작됐는지"/"자동 반려인지"를
        // 보여줘야 해서 auto_rejected와 함께 created_at도 여기서 주입한다. LocalDateTime 객체를
        // 그대로 넣지 않고 문자열로 바꾸는 이유: 이 맵을 최종적으로 직렬화하는 HTTP 메시지 컨버터가
        // 이 서비스가 쓰는 tools.jackson ObjectMapper와 별개라서, 타입을 안 타는 ISO 문자열이 더
        // 안전하다.
        Map<String, Object> withFlag = new LinkedHashMap<>(result);
        withFlag.put("auto_rejected", run.isAutoRejected());
        withFlag.put("created_at", run.getCreatedAt() == null ? null : run.getCreatedAt().toString());
        return withFlag;
    }

    private SalesTargetBatchRun findRun(String threadId) {
        return batchRunRepository.findById(threadId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_SALES_TARGET_BATCH_RUN));
    }

    private String requiredString(Map<String, Object> value, String key) {
        Object field = value.get(key);
        if (field == null || field.toString().isBlank()) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return field.toString();
    }

    private String write(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Map<String, Object> read(String value) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, e);
        }
    }
}
