package com.bp20.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductUpdateRequest(

        @NotBlank(message = "상품명은 필수입니다.")
        String productName,

        @NotBlank(message = "카테고리는 필수입니다.")
        String category,

        @NotBlank(message = "단위는 필수입니다.")
        String unit,

        @NotNull(message = "매입 가격은 필수입니다.")
        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "매입 가격은 0 이상이어야 합니다."
        )
        BigDecimal purchasePrice,

        @DecimalMin(
                value = "0.0",
                inclusive = true,
                message = "판매 가격은 0 이상이어야 합니다."
        )
        BigDecimal sellingPrice,

        @NotNull(message = "안전 재고는 필수입니다.")
        @Min(value = 0, message = "안전 재고는 0 이상이어야 합니다.")
        Integer safetyStock

) {
}