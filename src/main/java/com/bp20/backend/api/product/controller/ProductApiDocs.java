package com.bp20.backend.api.product.controller;

import com.bp20.backend.api.product.dto.request.CreateProductRequest;
import com.bp20.backend.api.product.dto.request.ProductStatusRequest;
import com.bp20.backend.api.product.dto.request.UpdateProductRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductApiDocs {

    @Operation(
            summary = "상품 등록",
            description = "매장에서 판매하는 상품을 공통 상품 원장에 등록합니다. 등록만으로 온라인에 노출되지는 않습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateProductRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "아메리카노",
                                            summary = "아메리카노 상품 등록",
                                                    value = """
                                                    {
                                                      "name": "아메리카노",
                                                      "description": "고소한 원두로 내린 시그니처 아메리카노입니다.",
                                                      "price": 4500,
                                                      "stockQuantity": 100,
                                                      "imageUrl": "https://cdn.bp20.com/products/americano.jpg"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "클럽 샌드위치",
                                            summary = "클럽 샌드위치 상품 등록",
                                                    value = """
                                                    {
                                                      "name": "클럽 샌드위치",
                                                      "description": "신선한 채소와 닭가슴살을 넣은 클럽 샌드위치입니다.",
                                                      "price": 6000,
                                                      "stockQuantity": 30,
                                                      "imageUrl": "https://cdn.bp20.com/products/club-sandwich.jpg"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    ResponseEntity<ApiResponse<ProductResponse>> create(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            CreateProductRequest request
    );

    @Operation(summary = "상품 목록 조회", description = "현재 점주 매장의 온·오프라인 공통 상품을 조회합니다.")
    ResponseEntity<ApiResponse<List<ProductResponse>>> getMine(
            @Parameter(hidden = true) SecurityPrincipal currentUser
    );

    @Operation(summary = "상품 상세 조회", description = "현재 점주 매장의 상품 한 건을 조회합니다.")
    ResponseEntity<ApiResponse<ProductResponse>> getOne(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            Long productId
    );

    @Operation(summary = "상품 수정", description = "상품명, 가격, 재고, 이미지 등의 공통 상품 정보를 수정합니다.")
    ResponseEntity<ApiResponse<ProductResponse>> update(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            Long productId,
            UpdateProductRequest request
    );

    @Operation(summary = "상품 상태 변경", description = "온·오프라인 공통 상품 상태를 활성, 비활성 또는 품절로 변경합니다.")
    ResponseEntity<ApiResponse<ProductResponse>> changeStatus(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            Long productId,
            ProductStatusRequest request
    );
}
