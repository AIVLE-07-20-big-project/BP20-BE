package com.bp20.backend.api.commerce.order.controller;

import com.bp20.backend.api.commerce.order.dto.request.CreateOnlinePurchaseRequest;
import com.bp20.backend.api.commerce.order.dto.response.OnlinePurchaseResponse;
import com.bp20.backend.api.commerce.order.service.OnlinePurchaseService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores/me/online-purchases")
@Tag(name = "점주 - O2O 구매 전환", description = "온라인 구매 이력을 확인하고 오프라인 방문 혜택으로 연결하는 API")
@SecurityRequirement(name = "bearerAuth")
public class OnlinePurchaseController {

    private final OnlinePurchaseService onlinePurchaseService;

    @PostMapping
    @Operation(
            summary = "온라인 결제 완료 이력 등록",
            description = "MVP에서 온라인 결제 완료 이벤트를 기록합니다. 운영 환경에서는 결제·주문 서비스가 이 역할을 담당합니다."
    )
    public ResponseEntity<ApiResponse<OnlinePurchaseResponse>> record(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateOnlinePurchaseRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PURCHASE_CREATE,
                onlinePurchaseService.record(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "온라인 구매 이력 조회", description = "고객과 구매 상품을 최신 구매 순서로 조회합니다.")
    public ResponseEntity<ApiResponse<List<OnlinePurchaseResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PURCHASE_GET,
                onlinePurchaseService.getMine(currentUser.id())
        );
    }
}
