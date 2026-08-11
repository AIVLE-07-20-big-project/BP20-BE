package com.bp20.backend.api.product.dto.request;

import com.bp20.backend.api.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "통합 상품 수정 요청")
public record UpdateProductRequest(
        @Schema(description = "상품명", example = "시그니처 클럽 샌드위치")
        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 120, message = "상품명은 120자 이하여야 합니다.")
        String name,

        @Schema(description = "상품 설명", example = "닭가슴살과 신선한 채소를 넣은 샌드위치입니다.")
        @Size(max = 2000, message = "상품 설명은 2,000자 이하여야 합니다.")
        String description,

        @Schema(description = "판매 가격(원)", example = "6500")
        @Positive(message = "판매 가격은 0원보다 커야 합니다.")
        long price,

        @Schema(description = "재고 수량. 수량을 관리하지 않는 상품은 null", example = "25", nullable = true)
        @PositiveOrZero(message = "재고 수량은 0개 이상이어야 합니다.")
        Integer stockQuantity,

        @Schema(description = "상품 이미지 URL", example = "https://cdn.bp20.com/products/signature-sandwich.jpg")
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @Schema(description = "판매 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SOLD_OUT"})
        ProductStatus status
) {
    public UpdateProductRequest(
            String name,
            String description,
            long price,
            Integer stockQuantity,
            String imageUrl
    ) {
        this(name, description, price, stockQuantity, imageUrl, null);
    }
}
