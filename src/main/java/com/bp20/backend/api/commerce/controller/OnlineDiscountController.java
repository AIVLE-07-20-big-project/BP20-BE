package com.bp20.backend.api.commerce.controller;

import com.bp20.backend.api.commerce.dto.request.CreateOnlineDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.OnlineDiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.response.OnlineDiscountResponse;
import com.bp20.backend.api.commerce.service.OnlineDiscountService;
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
@Tag(name = "점주 - 온라인 할인", description = "온라인 등록 상품과 세트상품의 할인 정책을 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class OnlineDiscountController {

    private final OnlineDiscountService onlineDiscountService;

    @PostMapping
    @Operation(
            summary = "온라인 할인 등록",
            description = "이미 온라인 판매에 등록된 상품 또는 세트상품을 선택해 할인 정책을 DRAFT 상태로 등록합니다."
    )
    public ResponseEntity<ApiResponse<OnlineDiscountResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateOnlineDiscountRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_DISCOUNT_CREATE,
                onlineDiscountService.create(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "온라인 할인 목록 조회", description = "현재 점주의 온라인 할인 정책을 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<OnlineDiscountResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_DISCOUNT_GET,
                onlineDiscountService.getMine(currentUser.id())
        );
    }

    @GetMapping("/{discountId}")
    @Operation(summary = "온라인 할인 상세 조회", description = "할인 조건과 대상 상품·세트상품을 조회합니다.")
    public ResponseEntity<ApiResponse<OnlineDiscountResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long discountId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_DISCOUNT_GET,
                onlineDiscountService.getOne(currentUser.id(), discountId)
        );
    }

    @PatchMapping("/{discountId}/status")
    @Operation(
            summary = "온라인 할인 상태 변경",
            description = "할인을 예약, 활성화, 일시중지 또는 종료합니다. 활성화 시 매장과 대상의 온라인 판매 상태를 검증합니다."
    )
    public ResponseEntity<ApiResponse<OnlineDiscountResponse>> changeStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long discountId,
            @Valid @RequestBody OnlineDiscountStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_DISCOUNT_UPDATE,
                onlineDiscountService.changeStatus(currentUser.id(), discountId, request)
        );
    }
}
