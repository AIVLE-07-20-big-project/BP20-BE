package com.bp20.backend.api.commerce.controller;

import com.bp20.backend.api.commerce.dto.request.OnlineSalesStatusRequest;
import com.bp20.backend.api.commerce.dto.response.ProductBundleResponse;
import com.bp20.backend.api.commerce.service.OnlineSalesService;
import com.bp20.backend.api.product.dto.request.OnlineSalesItemStatusRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.store.dto.response.StoreResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores/me/online-sales")
@Tag(name = "점주 - 온라인 판매", description = "매장의 온라인 판매 운영 상태를 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class OnlineSalesController {

    private final OnlineSalesService onlineSalesService;

    @PatchMapping("/status")
    @Operation(
            summary = "온라인 스토어 열기·닫기",
            description = "온라인 판매 상태를 OPEN 또는 CLOSED로 변경합니다. 온라인 판매 중인 상품 또는 세트상품이 하나 이상 있어야 열 수 있습니다."
    )
    public ResponseEntity<ApiResponse<StoreResponse>> changeStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody OnlineSalesStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_SALES_STATUS_UPDATE,
                onlineSalesService.changeStatus(currentUser.id(), request)
        );
    }

    @GetMapping("/products")
    @Operation(summary = "온라인 등록 상품 목록 조회", description = "공통 상품 중 온라인 판매에 등록된 상품만 조회합니다.")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRegisteredProducts(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_GET,
                onlineSalesService.getRegisteredProducts(currentUser.id())
        );
    }

    @PostMapping("/products/{productId}")
    @Operation(
            summary = "상품 온라인 판매 등록",
            description = "기존 공통 상품을 온라인 판매 상품으로 등록합니다. 활성 상태이고 재고가 있어야 합니다."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> registerProduct(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_REGISTER,
                onlineSalesService.registerProduct(currentUser.id(), productId)
        );
    }

    @PatchMapping("/products/{productId}/status")
    @Operation(summary = "온라인 상품 상태 변경", description = "온라인 상품을 판매, 품절 또는 숨김 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ProductResponse>> changeProductStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId,
            @Valid @RequestBody OnlineSalesItemStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_UPDATE,
                onlineSalesService.changeProductStatus(currentUser.id(), productId, request)
        );
    }

    @DeleteMapping("/products/{productId}")
    @Operation(summary = "상품 온라인 판매 해제", description = "상품 원장은 유지하고 온라인 판매 등록만 해제합니다.")
    public ResponseEntity<ApiResponse<ProductResponse>> unregisterProduct(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_UNREGISTER,
                onlineSalesService.unregisterProduct(currentUser.id(), productId)
        );
    }

    @PostMapping("/bundles/{bundleId}")
    @Operation(summary = "세트상품 온라인 판매 등록", description = "온·오프라인 공통 세트상품을 온라인 판매에 등록합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> registerBundle(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_REGISTER,
                onlineSalesService.registerBundle(currentUser.id(), bundleId)
        );
    }

    @PatchMapping("/bundles/{bundleId}/status")
    @Operation(summary = "온라인 세트상품 상태 변경", description = "온라인 세트상품을 판매, 품절 또는 숨김 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> changeBundleStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId,
            @Valid @RequestBody OnlineSalesItemStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_UPDATE,
                onlineSalesService.changeBundleStatus(currentUser.id(), bundleId, request)
        );
    }

    @DeleteMapping("/bundles/{bundleId}")
    @Operation(summary = "세트상품 온라인 판매 해제", description = "세트상품은 유지하고 온라인 판매 등록만 해제합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> unregisterBundle(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_ONLINE_PRODUCT_UNREGISTER,
                onlineSalesService.unregisterBundle(currentUser.id(), bundleId)
        );
    }
}
