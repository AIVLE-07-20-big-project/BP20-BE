package com.bp20.backend.api.commerce.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "세트상품 구성 상품 요청")
public record BundleItemRequest(
        @Schema(description = "매장 공통 상품 ID", example = "1")
        @NotNull(message = "구성 상품 ID는 필수입니다.")
        Long productId,

        @Schema(description = "세트에 포함되는 수량", example = "2")
        @Positive(message = "구성 상품 수량은 1개 이상이어야 합니다.")
        int quantity
) {
}
