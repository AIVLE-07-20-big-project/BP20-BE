package com.bp20.backend.api.commerce.controller;

import com.bp20.backend.api.commerce.dto.request.IssueCouponRequest;
import com.bp20.backend.api.commerce.dto.response.CustomerCouponResponse;
import com.bp20.backend.api.commerce.service.CustomerCouponService;
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
@RequestMapping("/api/store-owner/stores/me/coupons")
@Tag(name = "점주 - 고객 쿠폰", description = "특정 고객에게 할인 쿠폰을 직접 발급하고 상태를 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class CustomerCouponController {

    private final CustomerCouponService customerCouponService;

    @PostMapping
    @Operation(summary = "고객 쿠폰 발급", description = "고객과 할인 조건, 만료 시각을 지정해 쿠폰을 발급합니다.")
    public ResponseEntity<ApiResponse<CustomerCouponResponse>> issue(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_COUPON_ISSUE,
                customerCouponService.issue(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "발급 쿠폰 목록 조회", description = "현재 점주가 고객에게 발급한 쿠폰을 조회합니다.")
    public ResponseEntity<ApiResponse<List<CustomerCouponResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_COUPON_GET,
                customerCouponService.getMine(currentUser.id())
        );
    }

    @GetMapping("/{couponId}")
    @Operation(summary = "발급 쿠폰 상세 조회", description = "쿠폰 수령 고객과 발급·사용·취소 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<CustomerCouponResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long couponId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_COUPON_GET,
                customerCouponService.getOne(currentUser.id(), couponId)
        );
    }

    @PatchMapping("/{couponId}/revoke")
    @Operation(summary = "고객 쿠폰 발급 취소", description = "아직 사용되지 않은 고객 쿠폰을 발급 취소합니다.")
    public ResponseEntity<ApiResponse<CustomerCouponResponse>> revoke(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long couponId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_COUPON_UPDATE,
                customerCouponService.revoke(currentUser.id(), couponId)
        );
    }
}
