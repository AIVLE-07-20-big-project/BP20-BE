package com.bp20.backend.api.salestarget.controller;

import com.bp20.backend.api.salestarget.dto.request.StartSalesTargetBatchRequest;
import com.bp20.backend.api.salestarget.service.SalesTargetBatchService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 신규 가맹점 영업 타겟 배치(AI LangGraph 실행) 트리거 + 승인/반려.
 *
 * AI_Agent_전환_가이드라인.md 3단계 구현. SalesTargetController(후보 목록 조회/파이프라인
 * 상태 변경)와는 다른 컨트롤러로 분리했다 — 저건 이미 BE에 반영된 후보를 다루고, 이건 아직
 * BE에 반영되지 않은(승인 대기 중인) 배치 실행 자체를 다룬다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sales-targets/batches")
@Tag(name = "관리자 - 영업 타겟 배치 실행", description = "AI 배치 실행 트리거, 상태 조회, 승인/반려")
@SecurityRequirement(name = "bearerAuth")
public class SalesTargetBatchController {

    private final SalesTargetBatchService salesTargetBatchService;

    @Value("${sales-target.batch-cleanup.stale-days:3}")
    private int staleDays;

    @PostMapping
    @Operation(
            summary = "배치 실행 시작",
            description = "AI 서버가 후보를 스코어링할 때까지 동기로 기다린 뒤, 관리자 승인 대기 상태로 응답한다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> startBatch(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @RequestBody(required = false) StartSalesTargetBatchRequest request
    ) {
        Integer topN = request == null ? null : request.topN();
        return ApiResponse.success(
                SuccessCode.SUCCESS_SALES_TARGET_BATCH_START,
                salesTargetBatchService.startBatch(currentUser.id(), topN)
        );
    }

    @GetMapping
    @Operation(summary = "최근 배치 실행 목록 조회")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listBatches() {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_BATCH_LIST, salesTargetBatchService.listBatches());
    }

    @GetMapping("/{threadId}")
    @Operation(summary = "배치 실행 상태 조회", description = "승인 대기 중이면 후보 미리보기·주의사항을 함께 내려준다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBatch(@PathVariable String threadId) {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_BATCH_GET, salesTargetBatchService.getBatch(threadId));
    }

    @PostMapping("/{threadId}/approve")
    @Operation(
            summary = "배치 승인",
            description = "AI가 세일즈 피칭 문구를 생성하고 후보를 BE에 반영(bulk upsert)할 때까지 동기로 기다린다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveBatch(@PathVariable String threadId) {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_BATCH_APPROVE, salesTargetBatchService.approveBatch(threadId));
    }

    @PostMapping("/{threadId}/reject")
    @Operation(summary = "배치 반려", description = "이번 배치 결과는 BE에 반영하지 않고 폐기한다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectBatch(@PathVariable String threadId) {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_BATCH_REJECT, salesTargetBatchService.rejectBatch(threadId));
    }

    @DeleteMapping("/{threadId}")
    @Operation(
            summary = "배치 이력 한 건 삭제",
            description = "배치 이력 로그(캐시)만 지운다. 아직 승인 대기 중인 배치는 지울 수 없다(409) — 먼저 승인/반려해야 한다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable String threadId) {
        salesTargetBatchService.deleteBatchRun(threadId);
        return ApiResponse.successOnly(SuccessCode.SUCCESS_SALES_TARGET_BATCH_DELETE);
    }

    @PostMapping("/cleanup")
    @Operation(
            summary = "방치된 배치 수동 정리",
            description = "sales-target.batch-cleanup.stale-days(기본 3일) 이상 승인/반려되지 않은 배치를 즉시 자동 반려한다. "
                    + "평소엔 SalesTargetBatchCleanupScheduler가 주기적으로 대신 실행하며, 이 엔드포인트는 즉시 실행하고 싶을 때 관리자가 수동으로 호출한다."
    )
    public ResponseEntity<ApiResponse<List<String>>> cleanupStaleBatches() {
        return ApiResponse.success(
                SuccessCode.SUCCESS_SALES_TARGET_BATCH_CLEANUP,
                salesTargetBatchService.autoRejectStaleBatches(staleDays)
        );
    }
}
