package com.bp20.backend.api.productimage.controller;

import com.bp20.backend.api.productimage.dto.response.CategoriesResponse;
import com.bp20.backend.api.productimage.dto.response.ProductImageResponse;
import com.bp20.backend.api.productimage.service.ProductImageService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "ProductImage", description = "AI 상품 이미지 생성(배경 합성) API")
@RestController
@RequestMapping("/api/store-owner/product-images")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductImageController {

    private final ProductImageService productImageService;

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<CategoriesResponse>> categories() {
        CategoriesResponse result = productImageService.getCategories();
        return ApiResponse.success(SuccessCode.SUCCESS_PRODUCT_IMAGE_CATEGORIES, result);
    }

    /**
     * 상품 사진 + 메뉴명을 받아 배경이 합성된 이미지를 생성하고, 저장소(로컬 디스크 또는 S3)에
     * 저장한 뒤 접근 가능한 URL을 돌려준다. 어떤 상품에 적용할지는 이 URL로 기존 상품 수정
     * API(PUT /api/store-owner/stores/me/products/{productId})를 호출해서 정하면 된다.
     *
     * ⚠️ 호출 1건당 실제 비용(OpenAI API 과금)이 발생하고, 처리에 수 초~수십 초가 걸린다.
     */
    @PostMapping(value = "/generate", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ProductImageResponse>> generate(
            @RequestPart("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "prompt", required = false) String prompt
    ) {
        ProductImageResponse result = productImageService.generateAndStore(file, category, prompt);
        return ApiResponse.success(SuccessCode.SUCCESS_PRODUCT_IMAGE_GENERATE, result);
    }
}
