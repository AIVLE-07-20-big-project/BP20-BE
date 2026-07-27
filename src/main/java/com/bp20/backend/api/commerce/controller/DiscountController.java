package com.bp20.backend.api.commerce.controller;

import com.bp20.backend.api.commerce.dto.request.CreateDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.DiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.response.DiscountResponse;
import com.bp20.backend.api.commerce.service.DiscountService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores/me/discounts")
@Tag(name = "점주 - 할인", description = "온·오프라인 상품에 공통으로 적용할 할인을 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping
    @Operation(summary = "할인 등록", description = "등록된 상품을 대상으로 할인 정책을 DRAFT 상태로 등록합니다.")
    public ResponseEntity<ApiResponse<DiscountResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateDiscountRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_DISCOUNT_CREATE,
                discountService.create(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "할인 목록 조회", description = "현재 점주의 할인 정책을 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<DiscountResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_DISCOUNT_GET,
                discountService.getMine(currentUser.id())
        );
    }

    @GetMapping("/{discountId}")
    @Operation(summary = "할인 상세 조회", description = "할인 조건과 대상 상품을 조회합니다.")
    public ResponseEntity<ApiResponse<DiscountResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long discountId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_DISCOUNT_GET,
                discountService.getOne(currentUser.id(), discountId)
        );
    }

    @PatchMapping("/{discountId}/status")
    @Operation(
            summary = "할인 상태 변경",
            description = "할인을 예약, 활성화, 일시중지 또는 종료합니다. 활성화할 때 상품 상태와 적용 기간을 검증합니다."
    )
    public ResponseEntity<ApiResponse<DiscountResponse>> changeStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long discountId,
            @Valid @RequestBody DiscountStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_DISCOUNT_UPDATE,
                discountService.changeStatus(currentUser.id(), discountId, request)
        );
    }
}
