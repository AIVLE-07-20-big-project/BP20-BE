package com.bp20.backend.api.salestarget.controller;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import com.bp20.backend.api.salestarget.dto.request.UpdatePipelineStatusRequest;
import com.bp20.backend.api.salestarget.dto.response.SalesTargetCandidateResponse;
import com.bp20.backend.api.salestarget.service.SalesTargetService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/sales-targets")
@Tag(name = "관리자 - 신규 가맹점 영업 타겟", description = "AI가 계산한 영업 타겟 후보 조회 및 파이프라인 상태 관리")
public class SalesTargetController {

    private final SalesTargetService salesTargetService;

    @GetMapping
    @Operation(summary = "영업 타겟 후보 목록 조회", description = "status 쿼리 파라미터로 파이프라인 상태 필터링 가능. 없으면 전체 조회, 점수 내림차순 정렬.")
    public ResponseEntity<ApiResponse<List<SalesTargetCandidateResponse>>> getSalesTargets(
            @RequestParam(required = false) PipelineStatus status
    ) {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_GET, salesTargetService.getAll(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "영업 타겟 후보 상세 조회")
    public ResponseEntity<ApiResponse<SalesTargetCandidateResponse>> getSalesTarget(@PathVariable Long id) {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_GET, salesTargetService.getById(id));
    }

    @PatchMapping("/{id}/pipeline-status")
    @Operation(summary = "파이프라인 상태 변경", description = "영업 사원의 접촉/미팅/전환/보류/제외 진행 상황을 기록한다.")
    public ResponseEntity<ApiResponse<SalesTargetCandidateResponse>> updatePipelineStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePipelineStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_SALES_TARGET_STATUS_UPDATE,
                salesTargetService.updatePipelineStatus(id, request.pipelineStatus())
        );
    }

    @DeleteMapping
    @Operation(
            summary = "영업 타겟 후보 전체 삭제",
            description = "테스트/디버깅 중 쌓인 후보를 SQL 없이 한 번에 정리할 때 쓴다. pipelineStatus·sourceBatchId 관계없이 전부 삭제한다. "
                    + "배치 이력(승인/반려 기록)은 삭제하지 않는다."
    )
    public ResponseEntity<ApiResponse<Long>> deleteAllSalesTargets() {
        return ApiResponse.success(SuccessCode.SUCCESS_SALES_TARGET_DELETE_ALL, salesTargetService.deleteAll());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failOnly(ErrorCode.NOT_FOUND_SALES_TARGET));
    }
}
