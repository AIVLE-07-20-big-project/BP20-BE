package com.bp20.backend.api.product.controller;

import com.bp20.backend.api.product.dto.request.CreateProductRequest;
import com.bp20.backend.api.product.dto.request.ProductStatusRequest;
import com.bp20.backend.api.product.dto.request.UpdateProductRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.service.ProductService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
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
@RequestMapping("/api/store-owner/stores/me/products")
@Tag(name = "점주 - 상품 관리", description = "매장의 온·오프라인 공통 상품 원장을 관리하는 API")
@SecurityRequirement(name = "bearerAuth")
public class ProductController implements ProductApiDocs {

    private final ProductService productService;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_CREATE,
                productService.create(currentUser.id(), request)
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_GET,
                productService.getMine(currentUser.id())
        );
    }

    @Override
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_GET,
                productService.getOne(currentUser.id(), productId)
        );
    }

    @Override
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_UPDATE,
                productService.update(currentUser.id(), productId, request)
        );
    }

    @Override
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> changeStatus(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long productId,
            @Valid @RequestBody ProductStatusRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_PRODUCT_UPDATE,
                productService.changeStatus(currentUser.id(), productId, request)
        );
    }
}
