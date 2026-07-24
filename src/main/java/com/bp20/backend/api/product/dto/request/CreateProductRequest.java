package com.bp20.backend.api.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "온·오프라인 통합 상품 등록 요청")
public record CreateProductRequest(
        @Schema(description = "상품명", example = "클럽 샌드위치")
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 120, message = "상품명은 120자 이하여야 합니다.")
        String name,

        @Schema(description = "상품 설명", example = "신선한 채소와 닭가슴살을 넣은 클럽 샌드위치입니다.")
        @Size(max = 2000, message = "상품 설명은 2,000자 이하여야 합니다.")
        String description,

        @Schema(description = "판매 가격(원)", example = "6000")
        @Positive(message = "판매 가격은 0원보다 커야 합니다.")
        long price,

        @Schema(description = "재고 수량", example = "30")
        @PositiveOrZero(message = "재고 수량은 0개 이상이어야 합니다.")
        int stockQuantity,

        @Schema(description = "상품 이미지 URL", example = "https://cdn.bp20.com/products/club-sandwich.jpg")
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl
) {
}
