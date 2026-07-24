package com.bp20.backend.api.product.dto.request;

import com.bp20.backend.api.product.domain.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "상품 상태 변경 요청")
public record ProductStatusRequest(
        @NotNull(message = "상품 상태는 필수입니다.")
        @Schema(
                description = "온·오프라인 공통 상품 상태",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE", "SOLD_OUT"}
        )
        ProductStatus status
) {
}
