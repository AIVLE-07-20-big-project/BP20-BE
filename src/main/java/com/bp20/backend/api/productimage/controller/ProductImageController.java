package com.bp20.backend.api.productimage.controller;

import com.bp20.backend.api.productimage.dto.response.CategoriesResponse;
import com.bp20.backend.api.productimage.service.ProductImageService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
     * 상품 사진 + 메뉴명을 받아 배경이 합성된 이미지를 생성한다.
     * 다른 엔드포인트와 달리 ApiResponse로 감싸지 않고 이미지 바이너리를 그대로 반환한다
     * (ReceiptAnalyticsController의 HTML 리포트 엔드포인트와 동일한 패턴).
     *
     * ⚠️ 호출 1건당 실제 비용(OpenAI API 과금)이 발생하고, 처리에 수 초~수십 초가 걸린다.
     */
    @PostMapping(value = "/generate", consumes = "multipart/form-data", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generate(
            @RequestPart("file") MultipartFile file,
            @RequestParam("category") String category
    ) {
        byte[] imageBytes = productImageService.generateProductImage(file, category);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(imageBytes);
    }
}
