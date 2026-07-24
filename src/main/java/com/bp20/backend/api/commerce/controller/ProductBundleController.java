package com.bp20.backend.api.commerce.controller;

import com.bp20.backend.api.commerce.dto.request.BundleStatusRequest;
import com.bp20.backend.api.commerce.dto.request.CreateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.request.UpdateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.response.ProductBundleResponse;
import com.bp20.backend.api.commerce.service.ProductBundleService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores/me/bundles")
@Tag(name = "점주 - 세트상품", description = "여러 공통 상품과 수량을 묶어 온·오프라인 세트상품으로 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class ProductBundleController {

    private final ProductBundleService productBundleService;

    @PostMapping
    @Operation(summary = "세트상품 등록", description = "두 개 이상의 매장 상품을 수량과 함께 묶어 DRAFT 상태로 등록합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateProductBundleRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_BUNDLE_CREATE,
                productBundleService.create(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "세트상품 목록 조회", description = "현재 점주 매장의 온·오프라인 공통 세트상품을 조회합니다.")
    public ResponseEntity<ApiResponse<List<ProductBundleResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_BUNDLE_GET,
                productBundleService.getMine(currentUser.id())
        );
    }

    @GetMapping("/{bundleId}")
    @Operation(summary = "세트상품 상세 조회", description = "세트 가격과 구성 상품별 수량을 조회합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_BUNDLE_GET,
                productBundleService.getOne(currentUser.id(), bundleId)
        );
    }

    @PutMapping("/{bundleId}")
    @Operation(summary = "세트상품 수정", description = "세트상품 정보와 구성 상품별 수량을 수정합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> update(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId,
            @Valid @RequestBody UpdateProductBundleRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_BUNDLE_UPDATE,
                productBundleService.update(currentUser.id(), bundleId, request)
        );
    }

    @PatchMapping("/{bundleId}/status")
    @Operation(summary = "세트상품 상태 변경", description = "세트상품을 판매, 품절, 숨김 또는 초안 상태로 변경합니다.")
    public ResponseEntity<ApiResponse<ProductBundleResponse>> changeStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long bundleId,
            @Valid @RequestBody BundleStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_BUNDLE_UPDATE,
                productBundleService.changeStatus(currentUser.id(), bundleId, request)
        );
    }
}
