package com.bp20.backend.api.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "세트상품 등록 요청")
public record CreateProductBundleRequest(
        @Schema(description = "세트상품명", example = "브런치 커플 세트")
        @NotBlank(message = "세트상품명은 필수입니다.")
        @Size(max = 120, message = "세트상품명은 120자 이하여야 합니다.")
        String name,

        @Schema(description = "세트상품 설명", example = "아메리카노 2잔과 샌드위치 1개로 구성된 세트입니다.")
        @Size(max = 1000, message = "세트상품 설명은 1,000자 이하여야 합니다.")
        String description,

        @Schema(description = "세트 판매 가격(원)", example = "12000")
        @Positive(message = "세트 가격은 0원보다 커야 합니다.")
        long bundlePrice,

        @Schema(description = "세트상품 이미지 URL", example = "https://cdn.bp20.com/bundles/brunch-set.jpg")
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl,

        @Schema(
                description = "세트 구성 상품과 수량",
                example = "[{\"productId\": 1, \"quantity\": 2}, {\"productId\": 2, \"quantity\": 1}]"
        )
        @Valid
        @Size(min = 2, message = "세트 구성 상품은 두 개 이상이어야 합니다.")
        List<BundleItemRequest> items
) {
}
