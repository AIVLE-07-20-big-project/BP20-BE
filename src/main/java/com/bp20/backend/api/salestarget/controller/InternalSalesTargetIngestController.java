package com.bp20.backend.api.salestarget.controller;

import com.bp20.backend.api.salestarget.dto.request.BulkUpsertSalesTargetsRequest;
import com.bp20.backend.api.salestarget.dto.response.BulkUpsertResponse;
import com.bp20.backend.api.salestarget.service.SalesTargetService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/sales-targets")
@Tag(name = "내부 - 영업 타겟 배치 결과 반영", description = "AI 서버가 신규 가맹점 영업 타겟 배치 결과를 반영할 때 호출한다")
public class InternalSalesTargetIngestController {

    private final SalesTargetService salesTargetService;

    @PostMapping("/bulk")
    @Operation(
            summary = "배치 결과 벌크 반영(내부용)",
            description = "AI 파이프라인이 계산한 후보 목록을 반영한다. 기존 후보(businessName+address로 식별)는 " +
                    "점수만 갱신하고 pipelineStatus는 유지한다. X-Internal-Api-Key 헤더 필요."
    )
    public ResponseEntity<ApiResponse<BulkUpsertResponse>> bulkUpsert(
            @Valid @RequestBody BulkUpsertSalesTargetsRequest request
    ) {
        SalesTargetService.BulkUpsertResult result = salesTargetService.bulkUpsertCandidates(request);
        return ApiResponse.success(
                SuccessCode.SUCCESS_SALES_TARGET_BULK_UPSERT,
                new BulkUpsertResponse(result.created(), result.updated())
        );
    }
}
